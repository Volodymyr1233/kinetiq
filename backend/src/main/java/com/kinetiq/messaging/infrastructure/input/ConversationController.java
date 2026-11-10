package com.kinetiq.messaging.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations")
@RequiredArgsConstructor
class ConversationController {

    private final ExchangeMessagesUseCase messaging;
    private final MessagingWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /** Lists the signed-in person's conversations: a trainer's row per linked client, or a client's one row. */
    @GetMapping
    ConversationListResponse listConversations(Authentication authentication) {
        return mapper.toListResponse(messaging.listConversations(signedIn(authentication)));
    }

    /** The number of unread messages of the signed-in person, over all their conversations. */
    @GetMapping("/unread-count")
    UnreadCountResponse unreadCount(Authentication authentication) {
        return new UnreadCountResponse(messaging.unreadCount(signedIn(authentication)));
    }

    /**
     * One page of a conversation, the newest or the one before a message, each message with the logged set it is
     * about, if any. {@code 404} for an unknown client and another person's conversation alike.
     */
    @GetMapping("/{clientId}/messages")
    MessagePageResponse messages(
            @PathVariable UUID clientId,
            @RequestParam(required = false) UUID before,
            Locale locale,
            Authentication authentication) {
        AccountId person = signedIn(authentication);
        MessageId from = before == null ? null : new MessageId(before);
        return mapper.toResponse(messaging.messages(person, new AccountId(clientId), from), person, locale);
    }

    /**
     * Sends a message, which may be about one of the sender's own logged sets. Sending the same id, text and set again
     * answers the same, and stores nothing new. {@code 404} with {@code message.about-not-found} for a set that is not
     * a logged set of the sender: another client's, an unknown one, and any set named by a trainer.
     */
    @PostMapping("/{clientId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    MessageResponse send(
            @PathVariable UUID clientId,
            @Valid @RequestBody SendMessageRequest request,
            Locale locale,
            Authentication authentication) {
        AccountId person = signedIn(authentication);
        SetLogId about = request.aboutSetLogId() == null ? null : new SetLogId(request.aboutSetLogId());
        return mapper.toResponse(
                messaging.send(new SendMessageCommand(
                        person, new AccountId(clientId), new MessageId(request.id()), request.text(), about)),
                person,
                locale);
    }

    /** Marks a conversation read for the signed-in person. */
    @PutMapping("/{clientId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markRead(@PathVariable UUID clientId, Authentication authentication) {
        messaging.markRead(signedIn(authentication), new AccountId(clientId));
    }
}
