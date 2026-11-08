package com.kinetiq.plan.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftedPlan;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts, plans and draftings are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcDraftedPlanDirectory.class})
class JdbcDraftedPlanDirectoryIT {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");

    @Autowired
    private JdbcDraftedPlanDirectory directory;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertTrainer() {
        UUID id = UUID.randomUUID();
        insertAccount(id, "Trainer", "TRAINER", null);
        return new AccountId(id);
    }

    private AccountId insertClient(String name, AccountId trainer) {
        UUID id = UUID.randomUUID();
        insertAccount(id, name, "CLIENT", trainer.value());
        return new AccountId(id);
    }

    private void insertAccount(UUID id, String name, String role, UUID trainer) {
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, ?, ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                name,
                id + "@example.com",
                role,
                trainer,
                role);
    }

    private UUID insertPlan(UUID id, AccountId client, String mark, LocalDate firstWeek, Instant createdAt) {
        jdbc.update(
                """
                insert into plans (id, client_id, goal, weeks, first_week, published_on, automatic_draft, created_at,
                                   updated_at, version)
                values (?, ?, 'LOSE_FAT', 10, ?, ?, ?, ?, now(), 0)
                """,
                id,
                client.value(),
                firstWeek,
                firstWeek,
                mark,
                Timestamp.from(createdAt));
        return id;
    }

    private UUID insertDraft(AccountId client, String mark, Instant createdAt) {
        return insertPlan(UUID.randomUUID(), client, mark, null, createdAt);
    }

    private void insertSessions(UUID plan, String... days) {
        for (String day : days) {
            jdbc.update(
                    "insert into plan_sessions (id, plan_id, name, day_of_week) values (?, ?, 'Session', ?)",
                    UUID.randomUUID(),
                    plan,
                    day);
        }
    }

    private void insertDrafting(AccountId client) {
        jdbc.update("insert into plan_draftings (client_id, started_at) values (?, now())", client.value());
    }

    @Test
    void shouldListOnlyTheTrainersOwnDrafts() {
        AccountId trainer = insertTrainer();
        AccountId other = insertTrainer();
        AccountId withDraft = insertClient("Anna", trainer);
        AccountId published = insertClient("Basia", trainer);
        insertClient("Celina", trainer);
        AccountId othersClient = insertClient("Dorota", other);
        insertDraft(withDraft, "BUILT", NOW);
        insertPlan(UUID.randomUUID(), published, null, LocalDate.parse("2026-11-02"), NOW);
        insertDraft(othersClient, "BUILT", NOW);

        assertThat(directory.findOf(trainer)).extracting(DraftedPlan::client).containsExactly(withDraft);
        assertThat(directory.findOf(other)).extracting(DraftedPlan::client).containsExactly(othersClient);
    }

    @Test
    void shouldListOldestFirst_andEqualAgesByPlanId() {
        AccountId trainer = insertTrainer();
        AccountId newest = insertClient("Newest", trainer);
        AccountId second = insertClient("Second", trainer);
        AccountId first = insertClient("First", trainer);
        AccountId oldest = insertClient("Oldest", trainer);
        insertDraft(newest, "BUILT", NOW);
        insertPlan(UUID.fromString("00000000-0000-4000-9300-0000000000b2"), second, null, null, NOW.minusSeconds(60));
        insertPlan(UUID.fromString("00000000-0000-4000-9300-0000000000b1"), first, null, null, NOW.minusSeconds(60));
        insertDraft(oldest, "FAILED", NOW.minusSeconds(3600));

        assertThat(directory.findOf(trainer))
                .extracting(DraftedPlan::clientName)
                .containsExactly("Oldest", "First", "Second", "Newest");
    }

    @Test
    void shouldTellTheKindByTheMark() {
        AccountId trainer = insertTrainer();
        insertDraft(insertClient("Built", trainer), "BUILT", NOW.minusSeconds(3));
        insertDraft(insertClient("Failed", trainer), "FAILED", NOW.minusSeconds(2));
        insertDraft(insertClient("Saved", trainer), null, NOW.minusSeconds(1));

        assertThat(directory.findOf(trainer))
                .extracting(DraftedPlan::kind)
                .containsExactly(DraftedPlan.Kind.BUILT, DraftedPlan.Kind.FAILED, DraftedPlan.Kind.SAVED);
    }

    @Test
    void shouldCarryNameWeeksSessionsAndCreationTime() {
        AccountId trainer = insertTrainer();
        AccountId client = insertClient("Ola Kowalska", trainer);
        insertSessions(insertDraft(client, "BUILT", NOW), "MONDAY", "WEDNESDAY", "FRIDAY");

        DraftedPlan draft = directory.findOf(trainer).getFirst();

        assertThat(draft.clientName()).isEqualTo("Ola Kowalska");
        assertThat(draft.weeks()).isEqualTo(10);
        assertThat(draft.sessionsPerWeek()).isEqualTo(3);
        assertThat(draft.createdAt()).isEqualTo(NOW);
    }

    @Test
    void shouldCountNoSessions_forAnEmptyFailedDraft() {
        AccountId trainer = insertTrainer();
        insertDraft(insertClient("Failed", trainer), "FAILED", NOW);

        assertThat(directory.findOf(trainer).getFirst().sessionsPerWeek()).isZero();
    }

    // a first drafting has nothing to open yet; a drafting over a stored plan leaves that plan to review
    @Test
    void shouldLeaveOutAFirstDrafting_butKeepAPlanBeingDraftedAgain() {
        AccountId trainer = insertTrainer();
        AccountId first = insertClient("First drafting", trainer);
        AccountId again = insertClient("Drafted again", trainer);
        insertDrafting(first);
        insertDraft(again, "BUILT", NOW);
        insertDrafting(again);

        assertThat(directory.findOf(trainer)).extracting(DraftedPlan::client).containsExactly(again);
    }
}
