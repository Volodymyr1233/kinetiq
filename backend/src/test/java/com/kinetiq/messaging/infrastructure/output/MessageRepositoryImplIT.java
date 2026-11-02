package com.kinetiq.messaging.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.Conversation;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.ConversationPair;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.model.MessageText;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    MessageRepositoryImpl.class,
    ConversationRepositoryImpl.class,
    MessagingMapperImpl.class
})
class MessageRepositoryImplIT {

    private static final Instant AT = Instant.parse("2026-11-02T08:00:00Z");

    @Autowired
    private MessageRepositoryImpl messages;

    @Autowired
    private ConversationRepositoryImpl conversations;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactions;

    private MessagingFixture fixture;
    private AccountId trainer;
    private AccountId client;
    private ConversationId conversation;

    @BeforeEach
    void setUp() {
        fixture = new MessagingFixture(jdbc);
        trainer = fixture.trainer("Marek");
        client = fixture.client("Anna", trainer);
        conversation = new ConversationId(fixture.conversation(client, trainer));
    }

    private Message message(MessageId id, String text, Instant at) {
        return new Message(id, conversation, client, new MessageText(text), at);
    }

    private static MessageId newId() {
        return new MessageId(UUID.randomUUID());
    }

    private Instant lastRead(AccountId reader) {
        return jdbc.queryForObject(
                        "select last_read_at from conversation_reads where conversation_id = ? and account_id = ?",
                        OffsetDateTime.class,
                        conversation.value(),
                        reader.value())
                .toInstant();
    }

    @Nested
    class Saving {

        @Test
        void shouldStoreTheMessageAndReadItBack() {
            MessageId id = newId();

            assertThat(messages.saveIfNew(message(id, "first\nsecond", AT))).isTrue();

            Message found = messages.findById(id).orElseThrow();
            assertThat(found.getText().value()).isEqualTo("first\nsecond");
            assertThat(found.getSender()).isEqualTo(client);
            assertThat(found.getReceivedAt()).isEqualTo(AT);
        }

        @Test
        void shouldKeepTheFirstMessage_whenTheIdExists() {
            MessageId id = newId();
            messages.saveIfNew(message(id, "first", AT));

            assertThat(messages.saveIfNew(message(id, "other", AT.plusSeconds(1)))).isFalse();

            assertThat(messages.findById(id).orElseThrow().getText().value()).isEqualTo("first");
        }

        @Test
        void shouldBeRefusedByTheDatabase_whenTheTextIsTooLong() {
            assertThatThrownBy(() -> fixture.message(conversation.value(), client, "a".repeat(2001), AT))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        void shouldBeRefusedByTheDatabase_whenTheTextIsOnlySpaces() {
            assertThatThrownBy(() -> fixture.message(conversation.value(), client, "   ", AT))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    @Nested
    class Paging {

        private List<MessageId> insert(int count) {
            List<MessageId> ids = new ArrayList<>();
            for (int i = 0; i < count; i++) {
                MessageId id = newId();
                messages.saveIfNew(message(id, "message " + i, AT.plusSeconds(i)));
                ids.add(id);
            }
            return ids;
        }

        private static List<MessageId> ids(MessagePage page) {
            return page.messages().stream().map(Message::getId).toList();
        }

        @Test
        void shouldAnswerTheNewestPageOldestToNewest() {
            List<MessageId> all = insert(52);

            MessagePage page = messages.newest(conversation);

            assertThat(ids(page)).isEqualTo(all.subList(2, 52));
            assertThat(page.hasMore()).isTrue();
        }

        @Test
        void shouldHaveNoMore_whenExactlyOnePageExists() {
            insert(MessagePage.SIZE);

            assertThat(messages.newest(conversation).hasMore()).isFalse();
        }

        @Test
        void shouldAnswerTheMessagesBeforeTheGivenOne() {
            List<MessageId> all = insert(52);
            Message oldestLoaded = messages.findById(all.get(2)).orElseThrow();

            MessagePage page = messages.before(oldestLoaded);

            assertThat(ids(page)).isEqualTo(all.subList(0, 2));
            assertThat(page.hasMore()).isFalse();
        }

        // the time of a message can repeat, so only the id keeps the order stable
        @Test
        void shouldNeitherSkipNorRepeat_whenMessagesHaveTheSameTime() {
            List<UUID> sorted = new ArrayList<>();
            for (int i = 0; i < 55; i++) {
                MessageId id = newId();
                messages.saveIfNew(message(id, "same time " + i, AT));
                sorted.add(id.value());
            }
            sorted.sort(null);

            MessagePage newest = messages.newest(conversation);
            MessagePage earlier = messages.before(newest.messages().getFirst());

            List<UUID> seen = new ArrayList<>();
            earlier.messages().forEach(message -> seen.add(message.getId().value()));
            newest.messages().forEach(message -> seen.add(message.getId().value()));
            assertThat(seen).hasSize(55).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(sorted);
            assertThat(earlier.hasMore()).isFalse();
        }

        @Test
        void shouldNotMixConversations() {
            AccountId other = fixture.client("Basia", trainer);
            MessageId mine = newId();
            messages.saveIfNew(message(mine, "mine", AT));
            fixture.message(fixture.conversation(other, trainer), other, "theirs", AT);

            assertThat(ids(messages.newest(conversation))).containsExactly(mine);
        }
    }

    @Nested
    class MarkingRead {

        @Test
        void shouldStoreTheTime_whenReadForTheFirstTime() {
            conversations.markRead(conversation, client, AT);

            assertThat(lastRead(client)).isEqualTo(AT);
        }

        @Test
        void shouldNeverMoveTheTimeBack() {
            conversations.markRead(conversation, client, AT);

            conversations.markRead(conversation, client, AT.minusSeconds(60));

            assertThat(lastRead(client)).isEqualTo(AT);
        }

        @Test
        void shouldMoveTheTimeForward() {
            conversations.markRead(conversation, client, AT);

            conversations.markRead(conversation, client, AT.plusSeconds(60));

            assertThat(lastRead(client)).isEqualTo(AT.plusSeconds(60));
        }

        @Test
        void shouldLeaveTheOtherPersonsTimeAlone() {
            conversations.markRead(conversation, trainer, AT);

            conversations.markRead(conversation, client, AT.plusSeconds(60));

            assertThat(lastRead(trainer)).isEqualTo(AT);
        }
    }

    // committed for real, because the point is two transactions meeting in the database
    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    class InParallel {

        private void inParallel(Runnable first, Runnable second) throws Exception {
            TransactionTemplate transaction = new TransactionTemplate(transactions);
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                var one = executor.submit(() -> transaction.executeWithoutResult(status -> first.run()));
                var two = executor.submit(() -> transaction.executeWithoutResult(status -> second.run()));
                one.get();
                two.get();
            }
        }

        @Test
        void shouldBothSucceed_whenTwoPeopleMarkReadAtOnce() throws Exception {
            inParallel(
                    () -> conversations.markRead(conversation, client, AT),
                    () -> conversations.markRead(conversation, trainer, AT));

            assertThat(lastRead(client)).isEqualTo(AT);
            assertThat(lastRead(trainer)).isEqualTo(AT);
        }

        @Test
        void shouldBothSucceed_whenOnePersonMarksReadTwiceAtOnce() throws Exception {
            inParallel(
                    () -> conversations.markRead(conversation, client, AT),
                    () -> conversations.markRead(conversation, client, AT.plusSeconds(1)));

            assertThat(lastRead(client)).isEqualTo(AT.plusSeconds(1));
        }

        @Test
        void shouldEndWithOneConversation_whenTwoFirstSendsMeet() throws Exception {
            AccountId anotherClient = fixture.client("Basia", trainer);
            ConversationPair pair = new ConversationPair(anotherClient, trainer);

            inParallel(() -> conversations.findOrCreate(pair), () -> conversations.findOrCreate(pair));

            assertThat(jdbc.queryForObject(
                            "select count(*) from conversations where client_id = ?",
                            Integer.class,
                            anotherClient.value()))
                    .isEqualTo(1);
        }

        @Test
        void shouldStoreOneMessage_whenTwoAttemptsMeet() throws Exception {
            MessageId id = newId();

            inParallel(
                    () -> messages.saveIfNew(message(id, "once", AT)),
                    () -> messages.saveIfNew(message(id, "once", AT)));

            assertThat(jdbc.queryForObject("select count(*) from messages where id = ?", Integer.class, id.value()))
                    .isEqualTo(1);
        }
    }

    @Nested
    class FindingOrCreating {

        @Test
        void shouldReturnOneConversation_whenCalledTwice() {
            AccountId anotherClient = fixture.client("Basia", trainer);
            ConversationPair pair = new ConversationPair(anotherClient, trainer);

            Conversation first = conversations.findOrCreate(pair);
            Conversation second = conversations.findOrCreate(pair);

            assertThat(second.getId()).isEqualTo(first.getId());
            assertThat(second.getPair()).isEqualTo(pair);
        }

        @Test
        void shouldFindNothing_whenThePairHasNoConversation() {
            assertThat(conversations.find(new ConversationPair(fixture.client("Basia", trainer), trainer))).isEmpty();
        }
    }
}
