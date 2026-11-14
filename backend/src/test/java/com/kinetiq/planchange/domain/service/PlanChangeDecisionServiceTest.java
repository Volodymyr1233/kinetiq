package com.kinetiq.planchange.domain.service;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.exception.PlanChangeNothingToUndoException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.PlanSnapshots;
import com.kinetiq.planchange.domain.model.Side;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PlanChangeDecisionServiceTest {

    private final PlanChangeFixture world = new PlanChangeFixture();
    private final PlanChange stored = world.pendingFor(world.client);

    private final PlanChangeDecisionService service = new PlanChangeDecisionService(
            world.clients, world.accountsPort, world.planChanges, world.plans, world.messages, world.notices::add, world.clock);
    private final OwnPlanChangeDecisionService client = new OwnPlanChangeDecisionService(
            world.accountsPort, world.planChanges, world.plans, world.messages, world.notices::add, world.clock);

    private PlanChange current() {
        return world.planChanges.find(stored.getId()).orElseThrow();
    }

    @Test
    void shouldAcceptAsTheTrainer_andAnswerTheViewWithTheDecision() {
        PlanChangeView view = service.accept(world.trainer, stored.getId());

        assertThat(view.planChange().getState()).isEqualTo(PlanChangeState.ACCEPTED_BY_ONE);
        assertThat(view.planChange().acceptedBy(Side.TRAINER)).isTrue();
        assertThat(view.planChange().acceptedBy(Side.CLIENT)).isFalse();
        assertThat(view.clientName()).isEqualTo("Ola Nowak");
        assertThat(view.text().title()).isEqualTo("Back squat: cut volume by 10%");
        assertThat(current()).isSameAs(view.planChange());
    }

    @Test
    void shouldBeApplied_whenTheClientAcceptedFirstAndTheTrainerAccepts() {
        client.accept(world.client, stored.getId());

        PlanChangeView view = service.accept(world.trainer, stored.getId());

        assertThat(view.planChange().getState()).isEqualTo(PlanChangeState.APPLIED);
        assertThat(view.planChange().getEndedAt()).contains(world.clock.instant());
    }

    @Test
    void shouldBePending_whenTheTrainerUndoesTheirAcceptance() {
        service.accept(world.trainer, stored.getId());

        assertThat(service.undo(world.trainer, stored.getId()).planChange().getState())
                .isEqualTo(PlanChangeState.PENDING);
    }

    @Test
    void shouldRefuseUndo_whenThereIsNothingOfTheTrainersToUndo() {
        assertThatThrownBy(() -> service.undo(world.trainer, stored.getId()))
                .isInstanceOf(PlanChangeNothingToUndoException.class);

        client.accept(world.client, stored.getId());
        assertThatThrownBy(() -> service.undo(world.trainer, stored.getId()))
                .isInstanceOf(PlanChangeNothingToUndoException.class);
        assertThat(current().acceptedBy(Side.CLIENT)).isTrue();
    }

    @Test
    void shouldAnswerNotFound_forAnotherTrainersClientAndAnUnknownPlanChange() {
        assertThatThrownBy(() -> service.accept(world.otherTrainer, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.undo(world.otherTrainer, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.accept(world.trainer, PlanChangeId.random()))
                .isInstanceOf(PlanChangeNotFoundException.class);

        assertThat(current().getState()).isEqualTo(PlanChangeState.PENDING);
    }

    @Test
    void shouldAnswerNotFound_forAClientCallingAsTheTrainerWouldNot() {
        // a client is nobody's trainer, so the trainer path never finds their plan change
        assertThatThrownBy(() -> service.accept(world.client, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
    }

    @Test
    void shouldRefuse_whenItEnded() {
        client.accept(world.client, stored.getId());
        service.accept(world.trainer, stored.getId());

        assertThatThrownBy(() -> service.accept(world.trainer, stored.getId()))
                .isInstanceOf(PlanChangeEndedException.class);
        assertThatThrownBy(() -> service.undo(world.trainer, stored.getId()))
                .isInstanceOf(PlanChangeEndedException.class);
    }

    @Test
    void shouldRefuse_whenTheEffectiveWeekBeganInTheClientsTimeZone() {
        // 23:30 UTC on Sunday is already Monday in Warsaw
        world.clock.set("2026-11-15T23:30:00Z");

        assertThatThrownBy(() -> service.accept(world.trainer, stored.getId()))
                .isInstanceOf(PlanChangeWeekBegunException.class);
        assertThat(current().getState()).isEqualTo(PlanChangeState.PENDING);
    }

    @Test
    void shouldStillAccept_onTheSundayBeforeTheEffectiveWeek() {
        world.clock.set("2026-11-15T21:30:00Z");

        assertThat(service.accept(world.trainer, stored.getId()).planChange().getState())
                .isEqualTo(PlanChangeState.ACCEPTED_BY_ONE);
    }

    @Test
    void shouldAcceptTwiceWithoutChange_andKeepTheFirstAcceptanceTime() {
        service.accept(world.trainer, stored.getId());
        Instant first = current().getTrainerAcceptedAt().orElseThrow();

        world.clock.advance(java.time.Duration.ofHours(1));
        service.accept(world.trainer, stored.getId());

        assertThat(current().getTrainerAcceptedAt()).contains(first);
    }

    @Test
    void shouldNotEditThePlan_whenBothAcceptedAndItIsApplied() {
        PlanSnapshot plan = PlanSnapshots.plan(PlanSnapshots.session(
                MONDAY, PlanSnapshots.entry(PlanChangeFixture.SQUAT, 3, 10, "80")));
        world.plans.plans.put(world.client, plan);

        client.accept(world.client, stored.getId());
        service.accept(world.trainer, stored.getId());

        assertThat(current().getState()).isEqualTo(PlanChangeState.APPLIED);
        assertThat(world.plans.currentPlan(world.client)).contains(plan);
    }
}
