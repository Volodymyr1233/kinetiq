package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Side;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClientPlanChangeViewServiceTest {

    private final PlanChangeFixture world = new PlanChangeFixture();
    private final ClientPlanChangeViewService service =
            new ClientPlanChangeViewService(world.accountsPort, world.planChanges, world.plans);

    @Test
    void shouldShowTheOpenPlanChange_withTheTrainersNameAndTheText() {
        PlanChange stored = world.pendingFor(world.client);

        ClientPlanChangeView view = service.current(world.client);

        assertThat(view.planChange()).isSameAs(stored);
        assertThat(view.counterpartName()).isEqualTo("Marek Trainer");
        assertThat(view.text().title()).isEqualTo("Back squat: cut volume by 10%");
        assertThat(view.text().reason()).isEqualTo("Average RPE was 8.5 and 8.7 in the last 2 weeks, above 8.");
    }

    @Test
    void shouldWriteInPolish_forAPolishClient() {
        PlanChange stored = world.pendingFor(world.polishClient);

        ClientPlanChangeView view = service.get(world.polishClient, stored.getId());

        assertThat(view.text().title()).isEqualTo("Back squat PL: obniż objętość o 10%");
    }

    @Test
    void shouldAnswerNotFound_whenNoneIsOpen() {
        assertThatThrownBy(() -> service.current(world.client)).isInstanceOf(PlanChangeNotFoundException.class);
    }

    @Test
    void shouldShowAnEndedPlanChangeById_butNotAsTheCurrentOne() {
        PlanChange pending = world.pendingFor(world.client);
        PlanChange declined = pending.decline(
                Side.TRAINER, DeclineReason.INJURY, null, world.clock.instant(), LocalDate.parse("2026-11-12"));
        world.planChanges.update(declined);

        assertThat(service.get(world.client, pending.getId()).planChange().getState())
                .isEqualTo(PlanChangeState.DECLINED);
        assertThatThrownBy(() -> service.current(world.client)).isInstanceOf(PlanChangeNotFoundException.class);
    }

    @Test
    void shouldAnswerNotFound_forAnotherClientsPlanChange_asForAnUnknownOne() {
        PlanChange stored = world.pendingFor(world.client);

        assertThatThrownBy(() -> service.get(world.otherClient, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.get(world.polishClient, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.get(world.client, PlanChangeId.random()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.current(world.otherClient)).isInstanceOf(PlanChangeNotFoundException.class);
    }
}
