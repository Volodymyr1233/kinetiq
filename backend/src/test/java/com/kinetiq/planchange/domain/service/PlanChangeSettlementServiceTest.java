package com.kinetiq.planchange.domain.service;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.input.RevisePlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.planchange.domain.model.ExpiryCause;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.PlanSnapshots;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.Side;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanChangeSettlementServiceTest {

    // the content of the fixture lowers the amount of the squat on Monday from 10 to 9; its effective week begins on
    // Monday 16 November 2026, which is 15 November 23:00 UTC in Warsaw
    private static final String SUNDAY_BEFORE = "2026-11-15T22:30:00Z";
    private static final String MONDAY_IN_WARSAW = "2026-11-15T23:30:00Z";
    private static final long PLAN_VERSION = 3;

    private final PlanChangeFixture fixture = new PlanChangeFixture();
    private final RecordingRevisions revisions = new RecordingRevisions();
    private final PlanChangeSettlementService service = new PlanChangeSettlementService(
            fixture.accountsPort, fixture.planChanges, fixture.plans, revisions, fixture.clock);

    {
        fixture.planChanges.zones.put(fixture.client, PlanChangeFixture.WARSAW);
        fixture.planChanges.zones.put(fixture.otherClient, PlanChangeFixture.WARSAW);
    }

    private static final class RecordingRevisions implements RevisePlanUseCase {

        record Call(AccountId client, long expectedVersion, LocalDate effectiveWeek, List<Session> sessions) {}

        final List<Call> calls = new ArrayList<>();
        RuntimeException failure;

        @Override
        public void revise(AccountId client, long expectedVersion, LocalDate effectiveWeek, List<Session> sessions) {
            if (failure != null) {
                throw failure;
            }
            calls.add(new Call(client, expectedVersion, effectiveWeek, sessions));
        }
    }

    private PlanSnapshot planWithSquatAmount(int amount) {
        return PlanSnapshots.plan(
                session(MONDAY, entry(PlanChangeFixture.SQUAT, 3, amount, "60.0")),
                session(WEDNESDAY, entry(ExerciseId.random(), 3, 10, null)));
    }

    private PlanChange stateOf(PlanChange change) {
        return fixture.planChanges.find(change.getId()).orElseThrow();
    }

    private PlanChange pending() {
        fixture.plans.plans.put(fixture.client, planWithSquatAmount(10));
        return fixture.pendingFor(fixture.client);
    }

    private PlanChange acceptedByOne() {
        PlanChange change = pending();
        return fixture.planChanges.update(change.accept(Side.CLIENT, Instant.parse("2026-11-12T11:00:00Z"), LocalDate.parse("2026-11-12")));
    }

    private PlanChange upcoming() {
        PlanChange change = acceptedByOne();
        return fixture.planChanges.update(
                change.accept(Side.TRAINER, Instant.parse("2026-11-12T12:00:00Z"), LocalDate.parse("2026-11-12")));
    }

    @Test
    void shouldApplyAndKeepTheOldPlanInARevision_whenBothAcceptedAndTheMondayHasBegun() {
        PlanChange change = upcoming();
        fixture.clock.set(MONDAY_IN_WARSAW);

        assertThat(service.due()).containsExactly(change.getId());
        service.settle(change.getId());

        assertThat(stateOf(change).isInEffect()).isTrue();
        assertThat(stateOf(change).getTookEffectAt()).contains(Instant.parse(MONDAY_IN_WARSAW));
        assertThat(revisions.calls).singleElement().satisfies(call -> {
            assertThat(call.client()).isEqualTo(fixture.client);
            assertThat(call.expectedVersion()).isEqualTo(PLAN_VERSION);
            assertThat(call.effectiveWeek()).isEqualTo(LocalDate.parse("2026-11-16"));
            assertThat(call.sessions()).hasSize(2);
            ExerciseEntry squat = call.sessions().getFirst().entries().getFirst();
            assertThat(squat.amount()).isEqualTo(9);
            assertThat(squat.loadKg()).isEqualByComparingTo(new BigDecimal("60.0"));
        });
    }

    @Test
    void shouldNotApplyTheSundayBefore() {
        PlanChange change = upcoming();
        fixture.clock.set(SUNDAY_BEFORE);

        assertThat(service.due()).isEmpty();
        service.settle(change.getId());

        assertThat(stateOf(change).isUpcoming()).isTrue();
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldExpireAsUndecidedAndLeaveThePlan_whenNobodyAccepted() {
        PlanChange change = pending();
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).getState()).isEqualTo(PlanChangeState.EXPIRED);
        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.UNDECIDED);
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldExpireAsUndecidedAndLeaveThePlan_whenOnlyOnePersonAccepted() {
        PlanChange change = acceptedByOne();
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.UNDECIDED);
        assertThat(stateOf(change).acceptedBy(Side.CLIENT)).isTrue();
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldChangeNothingMore_whenSettledAgain() {
        PlanChange change = upcoming();
        fixture.clock.set(MONDAY_IN_WARSAW);
        service.settle(change.getId());
        PlanChange settled = stateOf(change);

        fixture.clock.advance(Duration.ofMinutes(5));
        service.settle(change.getId());

        assertThat(stateOf(change)).isSameAs(settled);
        assertThat(revisions.calls).hasSize(1);
        assertThat(service.due()).isEmpty();
    }

    @Test
    void shouldGiveTheSameResult_whenRunThreeDaysLate() {
        PlanChange change = upcoming();
        fixture.clock.set("2026-11-18T12:00:00Z");

        service.settle(change.getId());

        assertThat(stateOf(change).isInEffect()).isTrue();
        assertThat(revisions.calls).singleElement().satisfies(call -> {
            assertThat(call.effectiveWeek()).isEqualTo(LocalDate.parse("2026-11-16"));
            assertThat(call.sessions().getFirst().entries().getFirst().amount()).isEqualTo(9);
        });
    }

    @Test
    void shouldBeDueEarlierForAClientInAnEarlierTimeZone() {
        PlanChange change = pending();
        ZoneId auckland = ZoneId.of("Pacific/Auckland");
        Account warsaw = fixture.accounts.get(fixture.client);
        fixture.accounts.put(
                fixture.client,
                Account.restore(
                        warsaw.getId(), warsaw.getName(), new Email("ola@example.com"), "hash", Role.CLIENT,
                        warsaw.getTrainerId().orElse(null), auckland, AccountSettings.startingWith(Language.EN),
                        true, null, null, null, null));
        fixture.planChanges.zones.put(fixture.client, auckland);
        // Sunday 15 November 12:30 UTC is Monday 16 November 01:30 in Auckland and still Sunday in Warsaw
        fixture.clock.set("2026-11-15T12:30:00Z");

        assertThat(service.due()).containsExactly(change.getId());
        service.settle(change.getId());

        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.UNDECIDED);
    }

    @Test
    void shouldNotSettleBeforeTheMondayBeginsForTheClient_whenAskedTooEarly() {
        PlanChange change = pending();
        fixture.clock.set(SUNDAY_BEFORE);

        service.settle(change.getId());

        assertThat(stateOf(change).isOpen()).isTrue();
    }

    @Test
    void shouldExpireAsPlanEdited_whenATouchedValueWasEdited() {
        PlanChange change = upcoming();
        fixture.plans.plans.put(fixture.client, planWithSquatAmount(12));
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).getState()).isEqualTo(PlanChangeState.EXPIRED);
        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.PLAN_EDITED);
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldApplyAndKeepTheEdit_whenAnUnrelatedEntryWasEdited() {
        PlanChange change = upcoming();
        fixture.plans.plans.put(
                fixture.client,
                PlanSnapshots.plan(
                        session(MONDAY, entry(PlanChangeFixture.SQUAT, 3, 10, "60.0")),
                        session(WEDNESDAY, entry(ExerciseId.random(), 5, 6, "20.0"))));
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).isInEffect()).isTrue();
        assertThat(revisions.calls).singleElement().satisfies(call -> {
            assertThat(call.sessions().getFirst().entries().getFirst().amount()).isEqualTo(9);
            assertThat(call.sessions().get(1).entries().getFirst().sets()).isEqualTo(5);
        });
    }

    @Test
    void shouldExpireAsPlanEdited_whenTheClientHasNoPlanAnyMore() {
        PlanChange change = upcoming();
        fixture.plans.plans.remove(fixture.client);
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.PLAN_EDITED);
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldExpireAsPlanEdited_whenThePlanFinishedBeforeTheEffectiveWeek() {
        PlanChange change = upcoming();
        PlanSnapshot full = planWithSquatAmount(10);
        fixture.plans.plans.put(fixture.client, new PlanSnapshot(PLAN_VERSION, full.firstWeek(), 4, full.sessions()));
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.PLAN_EDITED);
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldExpireAsPlanEdited_whenTheResultWouldBreakAPlanRule() {
        PlanChange change = upcoming();
        fixture.plans.plans.put(
                fixture.client, PlanSnapshots.plan(session(MONDAY, entry(PlanChangeFixture.SQUAT, 3, 10, "60.0"))));
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(change.getId());

        assertThat(stateOf(change).getExpiryCause()).contains(ExpiryCause.PLAN_EDITED);
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldLeaveDeclinedInEffectAndExpiredOnesAlone() {
        PlanChange declined = fixture.planChanges.update(pending()
                .decline(Side.TRAINER, DeclineReason.OTHER, null, Instant.parse("2026-11-12T11:00:00Z"), LocalDate.parse("2026-11-12")));
        fixture.clock.set(MONDAY_IN_WARSAW);

        service.settle(declined.getId());

        assertThat(stateOf(declined)).isSameAs(declined);
        assertThat(service.due()).isEmpty();

        PlanChange inEffect = fixture.planChanges.update(fixture.pendingFor(fixture.otherClient)
                .accept(Side.CLIENT, Instant.parse("2026-11-12T11:00:00Z"), LocalDate.parse("2026-11-12"))
                .accept(Side.TRAINER, Instant.parse("2026-11-12T11:30:00Z"), LocalDate.parse("2026-11-12"))
                .takeEffect(Instant.parse("2026-11-15T23:10:00Z")));
        service.settle(inEffect.getId());
        assertThat(stateOf(inEffect)).isSameAs(inEffect);
        assertThat(revisions.calls).isEmpty();
    }

    @Test
    void shouldLeaveTheChangeUnsettledAndThePlanAlone_whenTheRevisionIsRefused() {
        PlanChange change = upcoming();
        revisions.failure = new PlanChangedException();
        fixture.clock.set(MONDAY_IN_WARSAW);

        assertThatThrownBy(() -> service.settle(change.getId())).isInstanceOf(PlanChangedException.class);

        assertThat(stateOf(change).isUpcoming()).isTrue();
        assertThat(revisions.calls).isEmpty();

        revisions.failure = null;
        service.settle(change.getId());
        assertThat(stateOf(change).isInEffect()).isTrue();
    }
}
