package com.kinetiq.messaging.infrastructure.input;

import static java.util.stream.Collectors.groupingBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.NoticeStored;
import com.kinetiq.messaging.domain.model.ConversationChange;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.session.SessionRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter.SseEventBuilder;

/**
 * The open event streams of this server, one per tab. A change reaches every stream of its person once the
 * transaction that made it has committed; a stream ends when its tab goes, its sign-in ends or the server stops.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class ConversationStreams {

    static final int MAX_PER_SIGN_IN = 10;

    // an evicted tab waits this long before it reconnects, so tabs over the cap cannot evict each other in a loop
    private static final long EVICTED_RECONNECT_MS = 30_000;

    private final SessionRepository<?> sessions;

    // a stuck client socket must never hold up the request that stored the message
    private final ExecutorService sending = Executors.newVirtualThreadPerTaskExecutor();

    // ponytail: one list scanned per change and per tick; index by person if streams reach the thousands
    private final List<Stream> open = new ArrayList<>();

    private record Stream(AccountId person, String signIn, SseEmitter emitter) {}

    /** Registers a stream of one sign-in; with {@value #MAX_PER_SIGN_IN} already open, the oldest of them is closed. */
    void add(AccountId person, String signIn, SseEmitter emitter) {
        Stream stream = new Stream(person, signIn, emitter);
        Stream evicted;
        synchronized (open) {
            List<Stream> ofSignIn = open.stream()
                    .filter(other -> other.signIn().equals(signIn))
                    .toList();
            evicted = ofSignIn.size() < MAX_PER_SIGN_IN ? null : ofSignIn.getFirst();
            if (evicted != null) {
                open.remove(evicted);
            }
            open.add(stream);
        }
        emitter.onCompletion(() -> remove(stream));
        emitter.onError(error -> remove(stream));
        // a normal end: EventSource reconnects by itself
        emitter.onTimeout(emitter::complete);
        if (evicted != null) {
            send(evicted, SseEmitter.event().reconnectTime(EVICTED_RECONNECT_MS));
            close(evicted);
        }
        // at once, so the browser sees the stream open and a buffering proxy shows
        send(stream, ping());
        log.debug("Conversation stream opened");
    }

    @TransactionalEventListener(fallbackExecution = true)
    void on(ConversationChange change) {
        String name =
                switch (change.kind()) {
                    case MESSAGE -> "message";
                    case READ -> "read";
                };
        String client = change.client().value().toString();
        for (Stream stream : snapshot()) {
            if (stream.person().equals(change.person())) {
                sending.execute(() -> send(stream, SseEmitter.event().name(name).data(client)));
            }
        }
    }

    // the one stream of a sign-in also carries notifications: a second one per tab would double the connections
    @TransactionalEventListener(fallbackExecution = true)
    void on(NoticeStored stored) {
        for (Stream stream : snapshot()) {
            if (stream.person().value().equals(stored.recipient())) {
                sending.execute(() -> send(stream, SseEmitter.event().name("notification").data("new")));
            }
        }
    }

    // a deleted session (sign-out, password reset, expiry) raises no event, so the streams look for it
    @Scheduled(fixedDelay = 25, timeUnit = TimeUnit.SECONDS)
    void heartbeat() {
        Map<String, List<Stream>> bySignIn = snapshot().stream().collect(groupingBy(Stream::signIn));
        bySignIn.forEach((signIn, streams) -> {
            boolean ended = sessions.findById(signIn) == null;
            streams.forEach(stream -> {
                if (ended) {
                    close(stream);
                } else {
                    send(stream, ping());
                }
            });
        });
    }

    // before the web server stops, so a graceful shutdown does not wait on streams that never end by themselves
    @EventListener(ContextClosedEvent.class)
    void closeAll() {
        snapshot().forEach(this::close);
    }

    // a named event, not a comment: EventSource hides comments, and the browser needs to see the stream is alive
    private static SseEventBuilder ping() {
        return SseEmitter.event().name("ping").data("ping");
    }

    private void send(Stream stream, SseEventBuilder event) {
        try {
            stream.emitter().send(event);
        } catch (IOException | IllegalStateException exception) {
            log.debug("Conversation stream not writable, closed");
            close(stream);
        }
    }

    private void close(Stream stream) {
        remove(stream);
        try {
            stream.emitter().complete();
        } catch (RuntimeException exception) {
            // already ended by the container; nothing left to close
        }
        log.debug("Conversation stream closed");
    }

    private void remove(Stream stream) {
        synchronized (open) {
            open.remove(stream);
        }
    }

    private List<Stream> snapshot() {
        synchronized (open) {
            return List.copyOf(open);
        }
    }
}
