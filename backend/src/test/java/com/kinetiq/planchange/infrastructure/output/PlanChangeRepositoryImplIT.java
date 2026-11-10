package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.SessionMove;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts and exercises are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, PlanChangeRepositoryImpl.class, PlanChangeMapperImpl.class})
class PlanChangeRepositoryImplIT {

    private static final Instant CREATED = Instant.parse("2026-11-09T02:00:00Z");
    private static final LocalDate EFFECTIVE = LocalDate.parse("2026-11-16");

    @Autowired
    private PlanChangeRepositoryImpl repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private AccountId insertClient() {
        UUID trainer = UUID.randomUUID();
        insertAccount(trainer, "TRAINER", null);
        UUID client = UUID.randomUUID();
        insertAccount(client, "CLIENT", trainer);
        return new AccountId(client);
    }

    private void insertAccount(UUID id, String role, UUID trainer) {
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Account', ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                id + "@example.com",
                role,
                trainer,
                role);
    }

    private ExerciseId insertExercise() {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into exercises (id, owner_id, name, category, equipment, measure, default_sets, default_amount,
                                       rest_seconds, cues, created_at, updated_at, version)
                values (?, null, ?, 'LEGS', '', 'REPS', 3, 10, 90, '', now(), now(), 0)
                """,
                id,
                "Exercise " + id);
        return new ExerciseId(id);
    }

    private static PlanChange planChange(
            AccountId client, PlanChangeState state, PlanChangeContent content, Instant endedAt) {
        return new PlanChange(
                PlanChangeId.random(),
                client,
                Rule.HIGH_RPE,
                state,
                4,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                EFFECTIVE,
                content,
                List.of(),
                Set.of(),
                CREATED,
                endedAt);
    }

    private PlanChange pending(AccountId client) {
        ExerciseId exercise = insertExercise();
        return planChange(
                client,
                PlanChangeState.PENDING,
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(DayOfWeek.MONDAY, 0),
                        exercise,
                        "Lower body",
                        Target.AMOUNT,
                        new BigDecimal("10.0"),
                        new BigDecimal("9.0")))),
                null);
    }

    @Test
    void shouldReadBackEverythingStored_targetsRemovedSessionMovesFiguresAndExercises() {
        AccountId client = insertClient();
        ExerciseId squat = insertExercise();
        ExerciseId row = insertExercise();
        PlanChangeContent content = new PlanChangeContent(
                List.of(
                        new TargetChange(
                                new EntryRef(DayOfWeek.MONDAY, 1),
                                squat,
                                "Lower body",
                                Target.LOAD_KG,
                                new BigDecimal("100.0"),
                                new BigDecimal("105.0")),
                        new TargetChange(
                                new EntryRef(DayOfWeek.MONDAY, 1),
                                squat,
                                "Lower body",
                                Target.AMOUNT,
                                new BigDecimal("8.0"),
                                new BigDecimal("7.0"))),
                Optional.of(new RemovedSession(DayOfWeek.FRIDAY, "Full body")),
                List.of(
                        new SessionMove(new EntryRef(DayOfWeek.FRIDAY, 0), DayOfWeek.MONDAY),
                        new SessionMove(new EntryRef(DayOfWeek.FRIDAY, 2), DayOfWeek.WEDNESDAY)));
        List<Figure> figures = List.of(
                Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-10-26"), new BigDecimal("8.500")),
                Figure.bestLoad(squat, LocalDate.parse("2026-10-26"), new BigDecimal("100.000")));
        PlanChange stored = new PlanChange(
                PlanChangeId.random(),
                client,
                Rule.STALLED_WEIGHT,
                PlanChangeState.PENDING,
                7,
                LocalDate.parse("2026-10-19"),
                LocalDate.parse("2026-11-08"),
                EFFECTIVE,
                content,
                figures,
                Set.of(squat, row),
                CREATED,
                null);

        repository.save(stored);
        entityManager.clear();
        PlanChange read = repository.find(stored.getId()).orElseThrow();

        assertThat(read)
                .usingRecursiveComparison()
                .withComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .ignoringCollectionOrderInFields("content.changes")
                .isEqualTo(stored);
        assertThat(read.getContent().changes())
                .extracting(TargetChange::target)
                .containsExactly(Target.AMOUNT, Target.LOAD_KG);
    }

    @Test
    void shouldRefuseASecondOpenPlanChangeForOneClient() {
        AccountId client = insertClient();
        repository.save(pending(client));
        PlanChange second = pending(client);

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldStoreAnOpenPlanChange_whenTheClientsEarlierOneHasEnded() {
        AccountId client = insertClient();
        PlanChange ended = pending(client);
        repository.save(planChange(
                client, PlanChangeState.DECLINED, ended.getContent(), Instant.parse("2026-11-03T10:00:00Z")));

        assertThat(repository.hasOpen(client)).isFalse();
        PlanChange open = repository.save(pending(client));

        assertThat(repository.hasOpen(client)).isTrue();
        assertThat(repository.findOpen(client)).map(PlanChange::getId).contains(open.getId());
    }

    @Test
    void shouldReportTheLatestEnd_orNoneBeforeAPlanChangeHasEnded() {
        AccountId client = insertClient();
        repository.save(pending(client));
        assertThat(repository.lastEndedAt(client)).isEmpty();

        PlanChangeContent content = pending(client).getContent();
        jdbc.update(
                "update plan_changes set state = 'EXPIRED', ended_at = ? where client_id = ?",
                Timestamp.from(Instant.parse("2026-11-02T23:00:00Z")),
                client.value());
        repository.save(planChange(client, PlanChangeState.DECLINED, content, Instant.parse("2026-10-20T10:00:00Z")));

        assertThat(repository.lastEndedAt(client)).contains(Instant.parse("2026-11-02T23:00:00Z"));
    }

    @Test
    void shouldNeverFindAnotherClientsOpenPlanChange() {
        AccountId client = insertClient();
        AccountId other = insertClient();
        repository.save(pending(other));

        assertThat(repository.findOpen(client)).isEmpty();
        assertThat(repository.hasOpen(client)).isFalse();
    }
}
