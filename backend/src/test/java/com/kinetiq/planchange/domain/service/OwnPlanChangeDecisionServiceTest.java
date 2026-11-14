package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.exception.PlanChangeNothingToUndoException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import org.junit.jupiter.api.Test;

class OwnPlanChangeDecisionServiceTest {

    private final PlanChangeFixture world = new PlanChangeFixture();
    private final PlanChange stored = world.pendingFor(world.client);

    private final OwnPlanChangeDecisionService service = new OwnPlanChangeDecisionService(
            world.accountsPort, world.planChanges, world.plans, world.messages, world.notices::add, world.clock);
    private final PlanChangeDecisionService trainer = new PlanChangeDecisionService(
            world.clients, world.accountsPort, world.planChanges, world.plans, world.messages, world.notices::add, world.clock);

    @Test
    void shouldTellTheClientAboutTheTrainersAcceptance_andNotTheTrainer() {
        trainer.accept(world.trainer, stored.getId());

        assertThat(world.notices)
                .containsExactly(new Notice(world.client.value(), null, NoticeKind.PLAN_CHANGE_DECISION));
    }

    @Test
    void shouldTellTheTrainerAboutTheClientsAcceptanceWithTheClient_andNotTheClient() {
        service.accept(world.client, stored.getId());

        assertThat(world.notices)
                .containsExactly(new Notice(world.trainer.value(), world.client.value(), NoticeKind.PLAN_CHANGE_DECISION));
    }

    @Test
    void shouldTellNobody_whenAnAcceptanceIsRepeatedOrTaken_back() {
        service.accept(world.client, stored.getId());
        world.notices.clear();

        service.accept(world.client, stored.getId());
        service.undo(world.client, stored.getId());

        assertThat(world.notices).isEmpty();
    }

    private PlanChange current() {
        return world.planChanges.find(stored.getId()).orElseThrow();
    }

    @Test
    void shouldAcceptAsTheClient_andAnswerTheViewWithTheTrainersName() {
        ClientPlanChangeView view = service.accept(world.client, stored.getId());

        assertThat(view.planChange().getState()).isEqualTo(PlanChangeState.ACCEPTED_BY_ONE);
        assertThat(view.planChange().acceptedBy(Side.CLIENT)).isTrue();
        assertThat(view.counterpartName()).isEqualTo("Marek Trainer");
        assertThat(view.text().title()).isEqualTo("Back squat: cut volume by 10%");
    }

    @Test
    void shouldBeApplied_whenTheTrainerAcceptedFirstAndTheClientAccepts() {
        trainer.accept(world.trainer, stored.getId());

        assertThat(service.accept(world.client, stored.getId()).planChange().getState())
                .isEqualTo(PlanChangeState.APPLIED);
    }

    @Test
    void shouldUndoOnlyTheClientsOwnAcceptance() {
        trainer.accept(world.trainer, stored.getId());
        assertThatThrownBy(() -> service.undo(world.client, stored.getId()))
                .isInstanceOf(PlanChangeNothingToUndoException.class);

        service.accept(world.client, stored.getId());
        // the plan change is applied now, so nobody can take anything back
        assertThatThrownBy(() -> service.undo(world.client, stored.getId()))
                .isInstanceOf(PlanChangeEndedException.class);
    }

    @Test
    void shouldBePendingAgain_whenTheClientUndoes() {
        service.accept(world.client, stored.getId());

        assertThat(service.undo(world.client, stored.getId()).planChange().getState())
                .isEqualTo(PlanChangeState.PENDING);
    }

    @Test
    void shouldAnswerNotFound_forAnotherClientsPlanChange_asForAMissingOne() {
        assertThatThrownBy(() -> service.accept(world.otherClient, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.accept(world.polishClient, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.undo(world.otherClient, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.accept(world.client, PlanChangeId.random()))
                .isInstanceOf(PlanChangeNotFoundException.class);

        assertThat(current().getState()).isEqualTo(PlanChangeState.PENDING);
    }

    @Test
    void shouldAnswerNotFound_whenTheTrainerCallsThroughTheClientPath() {
        assertThatThrownBy(() -> service.accept(world.trainer, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
    }

    @Test
    void shouldRefuse_whenTheEffectiveWeekBegan() {
        world.clock.set("2026-11-16T07:00:00Z");

        assertThatThrownBy(() -> service.accept(world.client, stored.getId()))
                .isInstanceOf(PlanChangeWeekBegunException.class);
    }

    @Test
    void shouldWriteThePolishClientsViewInPolish() {
        PlanChange polish = world.pendingFor(world.polishClient);

        ClientPlanChangeView view = service.accept(world.polishClient, polish.getId());

        assertThat(view.text().title()).isEqualTo("Back squat PL: obniż objętość o 10%");
        assertThat(view.exercises().get(PlanChangeFixture.SQUAT).name()).isEqualTo("Back squat PL");
    }
}
