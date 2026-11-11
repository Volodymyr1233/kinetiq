package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
import com.kinetiq.planchange.domain.input.ClientPlanChangeView;
import com.kinetiq.planchange.domain.input.KeepCurrentPlanCommand;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.Decision;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Side;
import org.junit.jupiter.api.Test;

class KeepCurrentPlanTest {

    private final PlanChangeFixture world = new PlanChangeFixture();
    private final PlanChange stored = world.pendingFor(world.client);

    private final PlanChangeDecisionService trainerService = new PlanChangeDecisionService(
            world.clients, world.accountsPort, world.planChanges, world.plans, world.messages, world.clock);
    private final OwnPlanChangeDecisionService clientService = new OwnPlanChangeDecisionService(
            world.accountsPort, world.planChanges, world.plans, world.messages, world.clock);

    private PlanChange current(PlanChange of) {
        return world.planChanges.find(of.getId()).orElseThrow();
    }

    private static KeepCurrentPlanCommand keep(AccountId person, PlanChangeId id, DeclineReason reason, String note) {
        return new KeepCurrentPlanCommand(person, id, reason, note);
    }

    @Test
    void shouldDeclineAndPostOneMessageFromTheTrainer_toTheConversationOfTheClient() {
        PlanChangeView view = trainerService.keepCurrent(
                keep(world.trainer, stored.getId(), DeclineReason.SCHEDULE, "  Away that week "));

        assertThat(view.planChange().getState()).isEqualTo(PlanChangeState.DECLINED);
        assertThat(view.planChange().getDecision())
                .contains(new Decision(Side.TRAINER, DeclineReason.SCHEDULE, "Away that week"));
        assertThat(view.planChange().getEndedAt()).contains(world.clock.instant());
        SendMessageCommand message = world.messages.only().orElseThrow();
        assertThat(message.person()).isEqualTo(world.trainer);
        assertThat(message.client()).isEqualTo(world.client);
        assertThat(message.text())
                .isEqualTo("Keeping the current plan instead of: Back squat: cut volume by 10%. Reason: schedule.\nAway that week");
    }

    @Test
    void shouldPostTheMessageFromTheClientInPolish_whenAPolishClientDeclines() {
        PlanChange polish = world.pendingFor(world.polishClient);

        ClientPlanChangeView view =
                clientService.keepCurrent(keep(world.polishClient, polish.getId(), DeclineReason.INJURY, null));

        assertThat(view.planChange().getDecision()).contains(new Decision(Side.CLIENT, DeclineReason.INJURY, null));
        SendMessageCommand message = world.messages.only().orElseThrow();
        assertThat(message.person()).isEqualTo(world.polishClient);
        assertThat(message.client()).isEqualTo(world.polishClient);
        assertThat(message.text())
                .isEqualTo("Zostaję przy obecnym planie zamiast: Back squat PL: obniż objętość o 10%. Powód: kontuzja.");
    }

    @Test
    void shouldRefuseASecondRequest_andPostNothingMore() {
        trainerService.keepCurrent(keep(world.trainer, stored.getId(), DeclineReason.OTHER, null));

        assertThatThrownBy(() -> trainerService.keepCurrent(keep(world.trainer, stored.getId(), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeEndedException.class);
        assertThatThrownBy(() -> clientService.keepCurrent(keep(world.client, stored.getId(), DeclineReason.TOO_HARD, null)))
                .isInstanceOf(PlanChangeEndedException.class);
        assertThat(world.messages.sent).hasSize(1);
    }

    @Test
    void shouldLeaveThePlanChangeOpen_whenTheMessageCannotBeSent() {
        world.messages.failure = new IllegalStateException("the message store is down");

        assertThatThrownBy(() -> clientService.keepCurrent(keep(world.client, stored.getId(), DeclineReason.OTHER, null)))
                .isInstanceOf(IllegalStateException.class);

        assertThat(current(stored).getState()).isEqualTo(PlanChangeState.PENDING);
        assertThat(current(stored).getDecision()).isEmpty();
    }

    @Test
    void shouldDeclineAfterOneAccepted_keepingTheAcceptanceAsHistory() {
        clientService.accept(world.client, stored.getId());

        PlanChangeView view =
                trainerService.keepCurrent(keep(world.trainer, stored.getId(), DeclineReason.TOO_EASY, null));

        assertThat(view.planChange().getState()).isEqualTo(PlanChangeState.DECLINED);
        assertThat(view.planChange().acceptedBy(Side.CLIENT)).isTrue();
    }

    @Test
    void shouldPostNothing_andAnswerNotFound_forAnOutsider() {
        assertThatThrownBy(() -> trainerService.keepCurrent(keep(world.otherTrainer, stored.getId(), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> clientService.keepCurrent(keep(world.otherClient, stored.getId(), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> clientService.keepCurrent(keep(world.trainer, stored.getId(), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> trainerService.keepCurrent(keep(world.trainer, PlanChangeId.random(), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeNotFoundException.class);

        assertThat(world.messages.sent).isEmpty();
        assertThat(current(stored).getState()).isEqualTo(PlanChangeState.PENDING);
    }

    @Test
    void shouldPostNothing_whenTheEffectiveWeekBegan() {
        world.clock.set("2026-11-16T07:00:00Z");

        assertThatThrownBy(() -> clientService.keepCurrent(keep(world.client, stored.getId(), DeclineReason.OTHER, null)))
                .isInstanceOf(PlanChangeWeekBegunException.class);
        assertThat(world.messages.sent).isEmpty();
    }
}
