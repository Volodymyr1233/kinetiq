package com.kinetiq.messaging.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.exception.ConversationNotFoundException;
import com.kinetiq.messaging.domain.exception.MessageEmptyException;
import com.kinetiq.messaging.domain.exception.MessageIdInUseException;
import com.kinetiq.messaging.domain.exception.MessageNotFoundException;
import com.kinetiq.messaging.domain.exception.MessageTooLongException;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessageText;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MessagingServiceTest {

    // nanoseconds on purpose: PostgreSQL keeps only microseconds
    private static final Instant NOW = Instant.parse("2026-11-02T09:12:00.123456789Z");
    private static final Instant NOW_IN_MICROS = Instant.parse("2026-11-02T09:12:00.123456Z");

    private final AccountId marek = AccountId.random();
    private final AccountId ola = AccountId.random();
    private final AccountId anna = AccountId.random();
    private final AccountId basia = AccountId.random();
    private final AccountId stranger = AccountId.random();

    private final MessagingFakes.Participants participants = new MessagingFakes.Participants();
    private final MessagingFakes.Conversations conversations = new MessagingFakes.Conversations();
    private final MessagingFakes.Messages messages = new MessagingFakes.Messages();

    private final MessagingService service = new MessagingService(
            participants, conversations, messages, new MessagingFakes.Directory(), Clock.fixed(NOW, ZoneOffset.UTC));

    @BeforeEach
    void setUp() {
        participants.link(anna, marek);
        participants.link(basia, ola);
    }

    private static MessageId newId() {
        return new MessageId(UUID.randomUUID());
    }

    private SendMessageCommand send(AccountId person, AccountId client, String text) {
        return new SendMessageCommand(person, client, newId(), text);
    }

    @Nested
    class AccessRule {

        @Test
        void shouldLetAClientAndTheirTrainerUseTheConversation() {
            service.send(send(anna, anna, "from the client"));
            service.send(send(marek, anna, "from the trainer"));

            assertThat(service.messages(anna, anna, null).messages()).hasSize(2);
            assertThat(service.messages(marek, anna, null).messages()).hasSize(2);
            assertThat(conversations.stored).hasSize(1);
        }

        private void assertNotFoundOnEveryOperation(AccountId person, AccountId client) {
            assertThatThrownBy(() -> service.messages(person, client, null))
                    .isInstanceOf(ConversationNotFoundException.class);
            assertThatThrownBy(() -> service.send(send(person, client, "hello")))
                    .isInstanceOf(ConversationNotFoundException.class);
            assertThatThrownBy(() -> service.markRead(person, client))
                    .isInstanceOf(ConversationNotFoundException.class);
            assertThat(conversations.stored).isEmpty();
            assertThat(messages.stored).isEmpty();
        }

        @Test
        void shouldAnswerNotFound_whenThePersonIsAThirdPerson() {
            assertNotFoundOnEveryOperation(stranger, anna);
        }

        @Test
        void shouldAnswerNotFound_whenTheClientIsAnotherTrainers() {
            assertNotFoundOnEveryOperation(marek, basia);
        }

        @Test
        void shouldAnswerNotFound_whenAClientAsksForAnotherClient() {
            assertNotFoundOnEveryOperation(anna, basia);
        }

        @Test
        void shouldAnswerNotFound_whenTheClientIsUnknown() {
            assertNotFoundOnEveryOperation(marek, AccountId.random());
        }

        @Test
        void shouldAnswerNotFound_whenATrainerNamesTheirOwnId() {
            assertNotFoundOnEveryOperation(marek, marek);
        }

        @Test
        void shouldAnswerNotFound_whenTheTrainerIsNoLongerLinked() {
            service.send(send(marek, anna, "before the move"));
            participants.link(anna, ola);

            assertThatThrownBy(() -> service.messages(marek, anna, null))
                    .isInstanceOf(ConversationNotFoundException.class);
        }

        // access decides first, so a stranger cannot tell a bad text from a missing conversation
        @Test
        void shouldAnswerNotFoundBeforeLookingAtTheText() {
            assertThatThrownBy(() -> service.send(send(stranger, anna, "")))
                    .isInstanceOf(ConversationNotFoundException.class);
        }
    }

    @Nested
    class Sending {

        @Test
        void shouldCreateTheConversationOnce() {
            service.send(send(anna, anna, "first"));
            service.send(send(anna, anna, "second"));

            assertThat(conversations.stored).hasSize(1);
            assertThat(messages.stored).hasSize(2);
        }

        @Test
        void shouldStoreTheSignedInPersonAsSenderAndTheTrimmedText() {
            Message sent = service.send(send(marek, anna, "  hello\nthere \n"));

            assertThat(sent.getSender()).isEqualTo(marek);
            assertThat(sent.getText().value()).isEqualTo("hello\nthere");
        }

        @Test
        void shouldStoreTheTimeInWholeMicroseconds() {
            assertThat(service.send(send(anna, anna, "hello")).getReceivedAt()).isEqualTo(NOW_IN_MICROS);
        }

        @Test
        void shouldReturnTheStoredMessage_whenTheSameIdAndTextAreSentAgain() {
            SendMessageCommand command = send(anna, anna, "hello");
            Message first = service.send(command);

            Message second = service.send(command);

            assertThat(second).isSameAs(first);
            assertThat(messages.stored).hasSize(1);
        }

        // the text is compared after trimming, as it was stored
        @Test
        void shouldReturnTheStoredMessage_whenTheSameTextDiffersOnlyInSpacesAround() {
            MessageId id = newId();
            service.send(new SendMessageCommand(anna, anna, id, "hello"));

            service.send(new SendMessageCommand(anna, anna, id, " hello "));

            assertThat(messages.stored).hasSize(1);
        }

        @Test
        void shouldRefuse_whenTheSameIdComesWithOtherText() {
            MessageId id = newId();
            service.send(new SendMessageCommand(anna, anna, id, "hello"));

            assertThatThrownBy(() -> service.send(new SendMessageCommand(anna, anna, id, "other")))
                    .isInstanceOf(MessageIdInUseException.class);
            assertThat(messages.stored.get(id).getText()).isEqualTo(new MessageText("hello"));
        }

        @Test
        void shouldRefuse_whenTheSameIdComesFromTheOtherPerson() {
            MessageId id = newId();
            service.send(new SendMessageCommand(anna, anna, id, "hello"));

            assertThatThrownBy(() -> service.send(new SendMessageCommand(marek, anna, id, "hello")))
                    .isInstanceOf(MessageIdInUseException.class);
        }

        @Test
        void shouldRefuse_whenTheSameIdComesFromAnotherConversation() {
            MessageId id = newId();
            service.send(new SendMessageCommand(anna, anna, id, "hello"));

            assertThatThrownBy(() -> service.send(new SendMessageCommand(basia, basia, id, "hello")))
                    .isInstanceOf(MessageIdInUseException.class);
        }

        @Test
        void shouldRefuseABadTextBeforeAnyConversationIsCreated() {
            assertThatThrownBy(() -> service.send(send(anna, anna, "   "))).isInstanceOf(MessageEmptyException.class);
            assertThatThrownBy(() -> service.send(send(anna, anna, "a".repeat(2001))))
                    .isInstanceOf(MessageTooLongException.class);

            assertThat(conversations.stored).isEmpty();
        }
    }

    @Nested
    class Reading {

        @Test
        void shouldAnswerAnEmptyPageAndStoreNothing_whenNobodyHasWritten() {
            assertThat(service.messages(anna, anna, null).messages()).isEmpty();
            assertThat(service.messages(anna, anna, null).hasMore()).isFalse();
            assertThat(conversations.stored).isEmpty();
        }

        @Test
        void shouldAnswerNotFound_whenBeforeIsAMessageOfAnotherConversation() {
            Message others = service.send(send(basia, basia, "theirs"));
            service.send(send(anna, anna, "mine"));

            assertThatThrownBy(() -> service.messages(anna, anna, others.getId()))
                    .isInstanceOf(MessageNotFoundException.class);
        }

        @Test
        void shouldAnswerNotFound_whenBeforeIsUnknown() {
            service.send(send(anna, anna, "mine"));

            assertThatThrownBy(() -> service.messages(anna, anna, newId()))
                    .isInstanceOf(MessageNotFoundException.class);
        }

        @Test
        void shouldAnswerNotFound_whenBeforeIsGivenAndNobodyHasWritten() {
            assertThatThrownBy(() -> service.messages(anna, anna, newId()))
                    .isInstanceOf(MessageNotFoundException.class);
        }

        @Test
        void shouldAnswerTheMessagesBeforeTheGivenOne() {
            Message earlier = new Message(
                    newId(),
                    conversations.findOrCreate(participants.pairOf(anna).orElseThrow()).getId(),
                    anna,
                    new MessageText("earlier"),
                    NOW_IN_MICROS.minusSeconds(60));
            messages.saveIfNew(earlier);
            Message later = service.send(send(anna, anna, "later"));

            assertThat(service.messages(marek, anna, later.getId()).messages()).containsExactly(earlier);
        }
    }

    @Nested
    class MarkingRead {

        @Test
        void shouldStoreNothing_whenThePairHasNoConversation() {
            service.markRead(anna, anna);

            assertThat(conversations.stored).isEmpty();
            assertThat(conversations.reads).isEmpty();
        }

        @Test
        void shouldStoreTheReadersTimeInWholeMicroseconds() {
            Message sent = service.send(send(marek, anna, "hello"));

            service.markRead(anna, anna);

            assertThat(conversations.reads.get(sent.getConversation())).containsOnlyKeys(anna);
            assertThat(conversations.reads.get(sent.getConversation()).get(anna)).isEqualTo(NOW_IN_MICROS);
        }
    }
}
