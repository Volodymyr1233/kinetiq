package com.kinetiq.messaging.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.awaitility.Awaitility.await;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.NoticeStored;
import com.kinetiq.messaging.domain.model.ConversationChange;
import com.kinetiq.messaging.domain.model.ConversationChange.Kind;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.session.MapSession;
import org.springframework.session.MapSessionRepository;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

class ConversationStreamsTest {

    private static final String PING = "event:ping\ndata:ping\n\n";

    private final MapSessionRepository sessions = new MapSessionRepository(new ConcurrentHashMap<>());
    private final ConversationStreams streams = new ConversationStreams(sessions);

    private final AccountId anna = AccountId.random();
    private final AccountId marek = AccountId.random();

    /** Keeps what was sent as SSE text and whether the stream was ended. */
    static class Recording extends SseEmitter {

        final List<String> sent = new CopyOnWriteArrayList<>();
        volatile boolean completed;

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            sent.add(builder.build().stream()
                    .map(part -> part.getData().toString())
                    .collect(Collectors.joining()));
        }

        @Override
        public void complete() {
            completed = true;
        }

        boolean received(String event) {
            return sent.stream().anyMatch(text -> text.contains(event));
        }
    }

    static class Broken extends Recording {

        @Override
        public void send(SseEventBuilder builder) throws IOException {
            throw new IOException("gone");
        }
    }

    private String signIn() {
        MapSession session = sessions.createSession();
        sessions.save(session);
        return session.getId();
    }

    private Recording open(AccountId person, String signIn) {
        Recording emitter = new Recording();
        streams.add(person, signIn, emitter);
        return emitter;
    }

    private static String message(AccountId client) {
        return "event:message\ndata:" + client.value();
    }

    @Test
    void shouldPingAtOnce_whenAStreamOpens() {
        Recording stream = open(anna, signIn());

        assertThat(stream.sent).containsExactly(PING);
    }

    @Test
    void shouldTellEveryStreamOfThePerson_onEverySignIn() {
        Recording phone = open(anna, signIn());
        Recording laptop = open(anna, signIn());
        Recording trainer = open(marek, signIn());

        streams.on(new ConversationChange(anna, anna, Kind.MESSAGE));

        await().until(() -> phone.received(message(anna)) && laptop.received(message(anna)));
        assertThat(trainer.received("event:message")).isFalse();
    }

    @Test
    void shouldTellOnlyThePersonNamed_whenAConversationIsRead() {
        Recording client = open(anna, signIn());
        Recording trainer = open(marek, signIn());

        streams.on(new ConversationChange(marek, anna, Kind.READ));

        await().until(() -> trainer.received("event:read\ndata:" + anna.value()));
        assertThat(client.received("event:read")).isFalse();
    }

    @Test
    void shouldTellEveryStreamOfTheRecipient_whenANotificationIsStored_andNoOtherPerson() {
        Recording phone = open(anna, signIn());
        Recording laptop = open(anna, signIn());
        Recording trainer = open(marek, signIn());

        streams.on(new NoticeStored(anna.value()));

        await().untilAsserted(() -> {
            assertThat(phone.received("event:notification\ndata:new")).isTrue();
            assertThat(laptop.received("event:notification\ndata:new")).isTrue();
        });
        assertThat(trainer.received("event:notification")).isFalse();
    }

    @Test
    void shouldDropAStreamThatCannotBeWrittenTo_whenANotificationIsStored() {
        Broken broken = new Broken();
        streams.add(anna, signIn(), broken);

        streams.on(new NoticeStored(anna.value()));

        await().untilAsserted(() -> assertThat(broken.completed).isTrue());
    }

    @Test
    void shouldCloseTheOldestOfASignIn_whenAnEleventhStreamOpens() {
        String signIn = signIn();
        List<Recording> tabs = IntStream.range(0, ConversationStreams.MAX_PER_SIGN_IN)
                .mapToObj(i -> open(anna, signIn))
                .toList();

        Recording eleventh = open(anna, signIn);
        streams.on(new ConversationChange(anna, anna, Kind.MESSAGE));

        Recording oldest = tabs.getFirst();
        assertThat(oldest.completed).isTrue();
        assertThat(oldest.received("retry:30000")).isTrue();
        await().until(() -> eleventh.received(message(anna))
                && tabs.stream().skip(1).allMatch(tab -> tab.received(message(anna))));
        assertThat(oldest.received("event:message")).isFalse();
    }

    @Test
    void shouldCloseAStream_whenItsSignInHasEnded() {
        String signIn = signIn();
        Recording ended = open(anna, signIn);
        Recording other = open(anna, signIn());
        sessions.deleteById(signIn);

        streams.heartbeat();
        streams.on(new ConversationChange(anna, anna, Kind.MESSAGE));

        assertThat(ended.completed).isTrue();
        await().until(() -> other.received(message(anna)));
        assertThat(ended.received("event:message")).isFalse();
        assertThat(other.completed).isFalse();
        assertThat(other.sent).filteredOn(PING::equals).hasSize(2);
    }

    @Test
    void shouldDropAStream_whenItCannotBeWrittenTo() {
        Broken broken = new Broken();
        streams.add(anna, signIn(), broken);
        Recording working = open(anna, signIn());

        assertThat(broken.completed).isTrue();
        assertThatCode(() -> streams.on(new ConversationChange(anna, anna, Kind.MESSAGE)))
                .doesNotThrowAnyException();
        await().until(() -> working.received(message(anna)));
    }

    @Test
    void shouldCloseEveryStream_whenTheServerStops() {
        Recording first = open(anna, signIn());
        Recording second = open(marek, signIn());

        streams.closeAll();

        assertThat(first.completed).isTrue();
        assertThat(second.completed).isTrue();
    }
}
