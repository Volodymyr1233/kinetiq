package com.kinetiq.client.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.quiz.domain.model.Goal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts and the quiz answers are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, JdbcClientDirectory.class})
class JdbcClientDirectoryIT {

    @Autowired
    private JdbcClientDirectory directory;

    @Autowired
    private JdbcTemplate jdbc;

    private AccountId insertTrainer() {
        UUID id = UUID.randomUUID();
        insertAccount(id, "Trainer " + id, "TRAINER", null);
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

    @Test
    void shouldListOnlyTheTrainersOwnClients() {
        AccountId marek = insertTrainer();
        AccountId ola = insertTrainer();
        AccountId anna = insertClient("Anna", marek);
        insertClient("Basia", ola);

        assertThat(directory.findClientsOf(marek)).extracting(ClientSummary::getId).containsExactly(anna);
        assertThat(directory.findClientsOf(ola)).extracting(ClientSummary::getName).containsExactly("Basia");
    }

    @Test
    void shouldFindOwnClientWithGoalAndPlanState() {
        AccountId trainer = insertTrainer();
        AccountId client = insertClient("Ola", trainer);
        insertQuiz(client, Goal.LOSE_FAT);

        ClientSummary found = directory.findClient(trainer, client).orElseThrow();

        assertThat(found.getName()).isEqualTo("Ola");
        assertThat(found.getGoal()).contains(Goal.LOSE_FAT);
        assertThat(found.getPlanState()).isEqualTo(PlanState.PLAN_TO_REVIEW);
    }

    // one answer for all three: another trainer's client, an account that is no client, and an unknown id
    @Test
    void shouldFindNothing_whenTheClientIsNotTheTrainersOwn() {
        AccountId marek = insertTrainer();
        AccountId ola = insertTrainer();
        AccountId olasClient = insertClient("Basia", ola);

        assertThat(directory.findClient(marek, olasClient)).isEmpty();
        assertThat(directory.findClient(marek, ola)).isEmpty();
        assertThat(directory.findClient(marek, AccountId.random())).isEmpty();
    }

    @Test
    void shouldListNothing_whenTrainerHasNoClients() {
        assertThat(directory.findClientsOf(insertTrainer())).isEmpty();
    }

    // a plain order by would put the capitals first
    @Test
    void shouldOrderByNameWithoutRegardToCase() {
        AccountId trainer = insertTrainer();
        insertClient("Zofia", trainer);
        insertClient("anna", trainer);
        insertClient("Bartek", trainer);

        assertThat(directory.findClientsOf(trainer))
                .extracting(ClientSummary::getName)
                .containsExactly("anna", "Bartek", "Zofia");
    }

    @Test
    void shouldOrderClientsWithTheSameNameById() {
        AccountId trainer = insertTrainer();
        UUID first = UUID.fromString("00000000-0000-4000-8000-0000000000a1");
        UUID second = UUID.fromString("00000000-0000-4000-8000-0000000000a2");
        insertAccount(second, "Anna", "CLIENT", trainer.value());
        insertAccount(first, "Anna", "CLIENT", trainer.value());

        assertThat(directory.findClientsOf(trainer))
                .extracting(summary -> summary.getId().value())
                .containsExactly(first, second);
    }

    @Test
    void shouldShowGoalAndPlanToReview_whenClientSubmittedTheQuiz() {
        AccountId trainer = insertTrainer();
        AccountId client = insertClient("Ola", trainer);
        insertQuiz(client, Goal.LOSE_FAT);

        ClientSummary summary = directory.findClientsOf(trainer).getFirst();

        assertThat(summary.getGoal()).contains(Goal.LOSE_FAT);
        assertThat(summary.getPlanState()).isEqualTo(PlanState.PLAN_TO_REVIEW);
    }

    @Test
    void shouldShowNoGoalAndNoQuiz_whenClientHasNotSubmittedTheQuiz() {
        AccountId trainer = insertTrainer();
        insertClient("Ola", trainer);

        ClientSummary summary = directory.findClientsOf(trainer).getFirst();

        assertThat(summary.getGoal()).isEmpty();
        assertThat(summary.getPlanState()).isEqualTo(PlanState.NO_QUIZ);
    }
}
