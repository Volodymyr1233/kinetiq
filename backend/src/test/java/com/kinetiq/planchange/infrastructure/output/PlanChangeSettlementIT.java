package com.kinetiq.planchange.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.ExpiryCause;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

// the accounts and exercises are inserted with SQL, as the migrations define them
@DataJpaTest
@Import({TestcontainersConfiguration.class, PlanChangeRepositoryImpl.class, PlanChangeMapperImpl.class})
class PlanChangeSettlementIT {

    private static final Instant CREATED = Instant.parse("2026-11-09T02:00:00Z");
    private static final Instant ACCEPTED = Instant.parse("2026-11-12T10:00:00Z");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");
    private static final LocalDate EFFECTIVE = LocalDate.parse("2026-11-16");

    @Autowired
    private PlanChangeRepositoryImpl repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    private AccountId insertClient(String zone) {
        UUID trainer = UUID.randomUUID();
        insertAccount(trainer, "TRAINER", null, "Europe/Warsaw");
        UUID client = UUID.randomUUID();
        insertAccount(client, "CLIENT", trainer, zone);
        return new AccountId(client);
    }

    private void insertAccount(UUID id, String role, UUID trainer, String zone) {
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, 'Account', ?, 'hash', ?, ?, ?, true, case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                id + "@example.com",
                role,
                trainer,
                zone,
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

    private PlanChange pending(AccountId client, LocalDate effectiveWeek) {
        return new PlanChange(
                PlanChangeId.random(),
                client,
                Rule.HIGH_RPE,
                PlanChangeState.PENDING,
                4,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                effectiveWeek,
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(DayOfWeek.MONDAY, 0),
                        insertExercise(),
                        "Lower body",
                        Target.AMOUNT,
                        new BigDecimal("10.0"),
                        new BigDecimal("9.0")))),
                List.of(),
                Set.of(),
                CREATED,
                null,
                null,
                null,
                null);
    }

    private PlanChange upcoming(AccountId client, LocalDate effectiveWeek) {
        return pending(client, effectiveWeek)
                .accept(Side.TRAINER, ACCEPTED, TODAY)
                .accept(Side.CLIENT, ACCEPTED, TODAY);
    }

    // cleared, so what is read back comes from the database and not from Hibernate's cached entity
    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void shouldReadBackWhenItTookEffect_andWhyItExpired() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange inEffect = repository.save(upcoming(client, EFFECTIVE));
        repository.update(inEffect.takeEffect(Instant.parse("2026-11-15T23:05:00.123456Z")));
        AccountId other = insertClient("Europe/Warsaw");
        PlanChange expired = repository.save(pending(other, EFFECTIVE));
        repository.update(expired.expire(ExpiryCause.UNDECIDED, Instant.parse("2026-11-15T23:05:00Z")));
        flushAndClear();

        PlanChange readInEffect = repository.find(inEffect.getId()).orElseThrow();
        assertThat(readInEffect.isInEffect()).isTrue();
        assertThat(readInEffect.getTookEffectAt()).contains(Instant.parse("2026-11-15T23:05:00.123456Z"));
        assertThat(readInEffect.getExpiryCause()).isEmpty();
        PlanChange readExpired = repository.find(expired.getId()).orElseThrow();
        assertThat(readExpired.getState()).isEqualTo(PlanChangeState.EXPIRED);
        assertThat(readExpired.getExpiryCause()).contains(ExpiryCause.UNDECIDED);
        assertThat(readExpired.getTookEffectAt()).isEmpty();
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenAStateThatIsNotAppliedHasTookEffect() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange stored = repository.save(pending(client, EFFECTIVE));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "update plan_changes set took_effect_at = now() where id = ?", stored.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_plan_changes_took_effect_applied");
    }

    // a refused statement aborts the transaction of the test, so each refusal has a test of its own
    @Test
    void shouldBeRefusedByTheDatabase_whenACauseIsSetWithoutExpiry() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange stored = repository.save(pending(client, EFFECTIVE));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "update plan_changes set expiry_cause = 'UNDECIDED' where id = ?", stored.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_plan_changes_expiry_cause_expired");
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenAnExpiredChangeHasNoCause() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange stored = repository.save(pending(client, EFFECTIVE));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "update plan_changes set state = 'EXPIRED', ended_at = now() where id = ?",
                        stored.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_plan_changes_expiry_cause_expired");
    }

    @Test
    void shouldBeRefusedByTheDatabase_whenTheCauseIsNotOneOfTheTwo() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange stored = repository.save(pending(client, EFFECTIVE));
        flushAndClear();

        assertThatThrownBy(() -> jdbc.update(
                        "update plan_changes set state = 'EXPIRED', expiry_cause = 'OTHER', ended_at = now() where id = ?",
                        stored.getId().value()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("ck_plan_changes_expiry_cause");
    }

    @Test
    void shouldCountAnUpcomingChangeAsUnsettled_andNotOnceItIsInEffectOrExpired() {
        AccountId client = insertClient("Europe/Warsaw");
        assertThat(repository.hasUnsettled(client)).isFalse();

        PlanChange upcoming = repository.save(upcoming(client, EFFECTIVE));
        flushAndClear();
        assertThat(repository.hasUnsettled(client)).isTrue();
        assertThat(repository.findOpen(client)).isEmpty();

        repository.update(upcoming.takeEffect(Instant.parse("2026-11-15T23:05:00Z")));
        flushAndClear();
        assertThat(repository.hasUnsettled(client)).isFalse();
    }

    @Test
    void shouldRefuseASecondUnsettledPlanChange_whenTheFirstIsUpcoming() {
        AccountId client = insertClient("Europe/Warsaw");
        repository.save(upcoming(client, EFFECTIVE));

        assertThatThrownBy(() -> repository.save(pending(client, EFFECTIVE.plusWeeks(1))))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uq_plan_changes_one_open_per_client");
    }

    @Test
    void shouldAllowAnotherPlanChange_whenTheFirstIsInEffectOrExpired() {
        AccountId inEffectClient = insertClient("Europe/Warsaw");
        PlanChange inEffect = repository.save(upcoming(inEffectClient, EFFECTIVE));
        repository.update(inEffect.takeEffect(Instant.parse("2026-11-15T23:05:00Z")));
        AccountId expiredClient = insertClient("Europe/Warsaw");
        PlanChange expired = repository.save(upcoming(expiredClient, EFFECTIVE));
        repository.update(expired.expire(ExpiryCause.PLAN_EDITED, Instant.parse("2026-11-15T23:05:00Z")));

        assertThat(repository.save(pending(inEffectClient, EFFECTIVE.plusWeeks(2)))).isNotNull();
        assertThat(repository.save(pending(expiredClient, EFFECTIVE.plusWeeks(2)))).isNotNull();
    }

    @Test
    void shouldFindDueByTheDateItIsForTheClient_atTheMondayBoundary() {
        AccountId auckland = insertClient("Pacific/Auckland");
        AccountId warsaw = insertClient("Europe/Warsaw");
        AccountId losAngeles = insertClient("America/Los_Angeles");
        PlanChange inAuckland = repository.save(pending(auckland, EFFECTIVE));
        PlanChange inWarsaw = repository.save(upcoming(warsaw, EFFECTIVE));
        PlanChange inLosAngeles = repository.save(pending(losAngeles, EFFECTIVE));
        flushAndClear();

        // 11:30 UTC on Sunday: Monday 00:30 in Auckland, Sunday elsewhere
        assertThat(repository.findDue(Instant.parse("2026-11-15T11:30:00Z"))).containsExactly(inAuckland.getId());
        // 23:30 UTC: Monday in Warsaw as well, Sunday in Los Angeles
        assertThat(repository.findDue(Instant.parse("2026-11-15T23:30:00Z")))
                .containsExactlyInAnyOrder(inAuckland.getId(), inWarsaw.getId());
        // 08:00 UTC on Monday: Monday 00:00 in Los Angeles
        assertThat(repository.findDue(Instant.parse("2026-11-16T08:00:00Z")))
                .containsExactlyInAnyOrder(inAuckland.getId(), inWarsaw.getId(), inLosAngeles.getId());
    }

    @Test
    void shouldFindADueChangeLongAfterItsMonday_andNotOnceItIsSettled() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange change = repository.save(upcoming(client, EFFECTIVE));
        flushAndClear();

        assertThat(repository.findDue(Instant.parse("2026-12-24T12:00:00Z"))).containsExactly(change.getId());

        repository.update(change.takeEffect(Instant.parse("2026-12-24T12:00:00Z")));
        flushAndClear();
        assertThat(repository.findDue(Instant.parse("2026-12-24T12:05:00Z"))).isEmpty();
    }

    @Test
    void shouldNotFindDeclinedChanges() {
        AccountId client = insertClient("Europe/Warsaw");
        PlanChange declined = pending(client, EFFECTIVE)
                .decline(Side.CLIENT, com.kinetiq.planchange.domain.model.DeclineReason.OTHER, null, ACCEPTED, TODAY);
        repository.save(declined);
        flushAndClear();

        assertThat(repository.findDue(Instant.parse("2026-12-24T12:00:00Z"))).isEmpty();
    }
}
