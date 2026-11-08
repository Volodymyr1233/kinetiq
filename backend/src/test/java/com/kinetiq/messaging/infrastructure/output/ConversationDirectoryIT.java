package com.kinetiq.messaging.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.LastMessage;
import com.kinetiq.messaging.domain.model.PlanWeek;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcConversationDirectory.class})
class ConversationDirectoryIT {

    private static final Instant MORNING = Instant.parse("2026-11-02T08:00:00Z");
    private static final Instant NOON = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant EVENING = Instant.parse("2026-11-02T18:00:00Z");

    @Autowired
    private JdbcConversationDirectory directory;

    @Autowired
    private JdbcTemplate jdbc;

    private MessagingFixture fixture;
    private AccountId marek;

    @BeforeEach
    void setUp() {
        fixture = new MessagingFixture(jdbc);
        marek = fixture.trainer("Marek");
    }

    private ConversationOverview only(AccountId person) {
        List<ConversationOverview> rows = directory.overviewOf(person);
        assertThat(rows).hasSize(1);
        return rows.getFirst();
    }

    @Test
    void shouldListEveryLinkedClientAndNoneOfAnotherTrainer() {
        AccountId anna = fixture.client("Anna", marek);
        AccountId basia = fixture.client("Basia", marek);
        fixture.client("Celina", fixture.trainer("Ola"));

        assertThat(directory.overviewOf(marek))
                .extracting(ConversationOverview::getClient)
                .containsExactly(anna, basia);
    }

    @Test
    void shouldHaveNoRowForTheTrainersOwnAccount_whenTrainerHasNoClients() {
        assertThat(directory.overviewOf(marek)).isEmpty();
        assertThat(directory.unreadTotal(marek)).isZero();
    }

    @Test
    void shouldOrderByLastMessageNewestFirst_thenClientsWithoutMessagesByName() {
        AccountId zofia = fixture.client("Zofia", marek);
        AccountId anna = fixture.client("anna", marek);
        AccountId earlier = fixture.client("Earlier", marek);
        AccountId later = fixture.client("Later", marek);
        fixture.message(fixture.conversation(earlier, marek), earlier, "first", MORNING);
        fixture.message(fixture.conversation(later, marek), later, "second", NOON);

        assertThat(directory.overviewOf(marek))
                .extracting(ConversationOverview::getName)
                .containsExactly("Later", "Earlier", "anna", "Zofia");
        assertThat(directory.overviewOf(marek)).element(2).satisfies(row -> {
            assertThat(row.getClient()).isEqualTo(anna);
            assertThat(row.getLastMessage()).isEmpty();
        });
        assertThat(directory.overviewOf(marek)).element(3).satisfies(row -> assertThat(row.getClient()).isEqualTo(zofia));
    }

    @Test
    void shouldGiveAClientOneRowWithTheTrainersName() {
        AccountId anna = fixture.client("Anna", marek);
        fixture.client("Basia", marek);

        ConversationOverview row = only(anna);

        assertThat(row.getClient()).isEqualTo(anna);
        assertThat(row.getName()).isEqualTo("Marek");
    }

    @Test
    void shouldCountAllTheOtherPersonsMessages_whenNeverRead() {
        AccountId anna = fixture.client("Anna", marek);
        UUID conversation = fixture.conversation(anna, marek);
        fixture.message(conversation, anna, "one", MORNING);
        fixture.message(conversation, anna, "two", NOON);
        fixture.message(conversation, marek, "mine", EVENING);

        assertThat(only(marek).getUnread()).isEqualTo(2);
        assertThat(only(anna).getUnread()).isEqualTo(1);
    }

    @Test
    void shouldCountOnlyMessagesAfterMyLastReadTime() {
        AccountId anna = fixture.client("Anna", marek);
        UUID conversation = fixture.conversation(anna, marek);
        fixture.message(conversation, anna, "seen", MORNING);
        fixture.message(conversation, anna, "exactly at the read time", NOON);
        fixture.message(conversation, anna, "new", EVENING);
        fixture.read(conversation, marek, NOON);

        assertThat(only(marek).getUnread()).isEqualTo(1);
    }

    @Test
    void shouldCountNothing_afterReadingEverything() {
        AccountId anna = fixture.client("Anna", marek);
        UUID conversation = fixture.conversation(anna, marek);
        fixture.message(conversation, anna, "one", MORNING);
        fixture.read(conversation, marek, NOON);

        assertThat(only(marek).getUnread()).isZero();
        assertThat(directory.unreadTotal(marek)).isZero();
    }

    @Test
    void shouldNotUseTheOtherPersonsReadTime() {
        AccountId anna = fixture.client("Anna", marek);
        UUID conversation = fixture.conversation(anna, marek);
        fixture.message(conversation, anna, "one", MORNING);
        fixture.read(conversation, anna, EVENING);

        assertThat(only(marek).getUnread()).isEqualTo(1);
    }

    @Test
    void shouldSumTheRows_whenAskedForTheTotal() {
        AccountId anna = fixture.client("Anna", marek);
        AccountId basia = fixture.client("Basia", marek);
        fixture.client("Celina", marek);
        fixture.message(fixture.conversation(anna, marek), anna, "one", MORNING);
        UUID withBasia = fixture.conversation(basia, marek);
        fixture.message(withBasia, basia, "two", MORNING);
        fixture.message(withBasia, basia, "three", NOON);

        assertThat(directory.unreadTotal(marek)).isEqualTo(3);
        assertThat(directory.overviewOf(marek)).extracting(ConversationOverview::getUnread).containsExactly(2, 1, 0);
    }

    // the earlier trainer's conversation stays stored; neither the new trainer nor its messages come into it
    @Test
    void shouldShowAnEmptyConversation_whenTheClientMovedToAnotherTrainer() {
        AccountId ola = fixture.trainer("Ola");
        AccountId anna = fixture.client("Anna", marek);
        fixture.message(fixture.conversation(anna, marek), anna, "to Marek", MORNING);

        jdbc.update("update accounts set trainer_id = ? where id = ?", ola.value(), anna.value());

        assertThat(directory.overviewOf(marek)).isEmpty();
        assertThat(directory.unreadTotal(marek)).isZero();
        assertThat(only(ola).getLastMessage()).isEmpty();
        assertThat(only(ola).getUnread()).isZero();
        assertThat(only(anna).getName()).isEqualTo("Ola");
        assertThat(only(anna).getLastMessage()).isEmpty();
    }

    @Test
    void shouldShowTheLastMessageFromTheViewersSide() {
        AccountId anna = fixture.client("Anna", marek);
        UUID conversation = fixture.conversation(anna, marek);
        fixture.message(conversation, marek, "old", MORNING);
        fixture.message(conversation, anna, "newest", NOON);

        LastMessage forTrainer = only(marek).getLastMessage().orElseThrow();
        LastMessage forClient = only(anna).getLastMessage().orElseThrow();

        assertThat(forTrainer.preview()).isEqualTo("newest");
        assertThat(forTrainer.sentByViewer()).isFalse();
        assertThat(forTrainer.receivedAt()).isEqualTo(NOON);
        assertThat(forClient.sentByViewer()).isTrue();
    }

    @Test
    void shouldCutThePreviewTo140Characters() {
        AccountId anna = fixture.client("Anna", marek);
        fixture.message(fixture.conversation(anna, marek), anna, "a".repeat(141), MORNING);

        assertThat(only(marek).getLastMessage().orElseThrow().preview()).hasSize(140);
    }

    @Test
    void shouldShowTheQuizGoal_whenThereIsNoPlan() {
        AccountId anna = fixture.client("Anna", marek);
        insertQuiz(anna, Goal.LOSE_FAT);

        assertThat(only(marek).getGoal()).contains(Goal.LOSE_FAT);
        assertThat(only(marek).getPlanWeek()).isEmpty();
    }

    @Test
    void shouldShowThePlansGoalAndCurrentWeek_whenThePlanIsPublished() {
        AccountId anna = fixture.client("Anna", marek);
        insertQuiz(anna, Goal.LOSE_FAT);
        LocalDate monday =
                LocalDate.now(ZoneId.of("Europe/Warsaw")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        insertPlan(anna, Goal.GET_STRONGER, monday.minusWeeks(2));

        assertThat(only(marek).getGoal()).contains(Goal.GET_STRONGER);
        assertThat(only(marek).getPlanWeek()).contains(new PlanWeek(3, 8));
    }

    @Test
    void shouldShowNoGoalOrWeek_onTheClientsOwnRow() {
        AccountId anna = fixture.client("Anna", marek);
        LocalDate monday =
                LocalDate.now(ZoneId.of("Europe/Warsaw")).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        insertPlan(anna, Goal.GET_STRONGER, monday);

        assertThat(only(anna).getGoal()).isEmpty();
        assertThat(only(anna).getPlanWeek()).isEmpty();
    }

    @Test
    void shouldShowNoWeek_whenThePlanIsADraft() {
        AccountId anna = fixture.client("Anna", marek);
        insertPlan(anna, Goal.GET_STRONGER, null);

        assertThat(only(marek).getGoal()).contains(Goal.GET_STRONGER);
        assertThat(only(marek).getPlanWeek()).isEmpty();
    }

    private void insertQuiz(AccountId client, Goal goal) {
        jdbc.update(
                """
                insert into quiz_submissions (id, client_id, goal, experience, level, age, height_cm, weight_kg,
                                              days_per_week, notes, submitted_at, created_at, updated_at, version)
                values (?, ?, ?, 'YEARS_1_TO_2', 'INTERMEDIATE', 29, 168, 62.5, 3, null, now(), now(), now(), 0)
                """,
                UUID.randomUUID(),
                client.value(),
                goal.name());
    }

    private void insertPlan(AccountId client, Goal goal, LocalDate firstWeek) {
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, created_at, updated_at,
                                   version)
                values (?, ?, ?, 8, ?::date, ?::date, now(), now(), 0)
                """,
                UUID.randomUUID(),
                client.value(),
                goal.name(),
                firstWeek == null ? null : firstWeek.toString(),
                firstWeek == null ? null : firstWeek.toString());
    }
}
