package com.kinetiq.messaging.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
class ConversationStreamController {

    // a stream ends now and then, so EventSource reconnects and the sign-in is used and checked again
    private static final Duration LIFETIME = Duration.ofMinutes(30);

    private final ConversationStreams streams;

    /**
     * The signed-in person's stream of events about their own conversations, as {@code text/event-stream}. Event
     * {@code message} says a message was stored in a conversation, {@code read} that this person marked one read; the
     * data of both is the conversation's client id, nothing else. Event {@code ping} comes at once and every 25
     * seconds, so the browser can tell a live stream from a dead one. It ends after 30 minutes and EventSource
     * reconnects.
     */
    @GetMapping(path = "/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    SseEmitter events(Authentication authentication, HttpSession session) {
        SseEmitter emitter = new SseEmitter(LIFETIME.toMillis());
        // sign-in stores the account id as the principal's name; the session is the sign-in
        streams.add(new AccountId(UUID.fromString(authentication.getName())), session.getId(), emitter);
        return emitter;
    }
}
