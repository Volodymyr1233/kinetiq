package com.kinetiq.planchange.domain.service;

import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanChangeViewServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId POLISH_TRAINER = AccountId.random();
    private static final AccountId OTHER_TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final ExerciseId SQUAT = ExerciseId.random();

    private final FakePlanChangeRepository planChanges = new FakePlanChangeRepository();
    private final FakePlanSource plans = new FakePlanSource();

    // the client belongs to both trainers named here but not to the other one
    private final ViewClientsUseCase clients = new ViewClientsUseCase() {
        @Override
        public List<ClientSummary> listClients(AccountId trainer) {
            return List.of();
        }

        @Override
        public ClientSummary getClient(AccountId trainer, AccountId client) {
            if (trainer.equals(OTHER_TRAINER) || !client.equals(CLIENT)) {
                throw new ClientNotFoundException();
            }
            return new ClientSummary(CLIENT, "Ola Nowak", Goal.LOSE_FAT, PlanState.PUBLISHED, WARSAW);
        }
    };

    private final CurrentAccountUseCase accounts = new CurrentAccountUseCase() {
        @Override
        public Account find(AccountId id) {
            Language language = id.equals(POLISH_TRAINER) ? Language.PL : Language.EN;
            return Account.signUpTrainer("Trainer", new Email("trainer@example.com"), "hash", WARSAW, language);
        }

        @Override
        public void changeTimeZone(AccountId id, ZoneId zone) {}

        @Override
        public Account update(AccountId id, UpdateAccountCommand command) {
            return find(id);
        }
    };

    private final PlanChangeViewService service = new PlanChangeViewService(clients, accounts, planChanges, plans);

    private final PlanChange stored = planChanges.save(new PlanChange(
            PlanChangeId.random(),
            CLIENT,
            Rule.HIGH_RPE,
            PlanChangeState.PENDING,
            1,
            LocalDate.parse("2026-10-26"),
            LocalDate.parse("2026-11-08"),
            LocalDate.parse("2026-11-16"),
            PlanChangeContent.ofChanges(List.of(new TargetChange(
                    new EntryRef(MONDAY, 0), SQUAT, "Lower", Target.AMOUNT, BigDecimal.TEN, BigDecimal.valueOf(9)))),
            List.of(
                    Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-10-26"), new BigDecimal("8.5")),
                    Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-11-02"), new BigDecimal("8.7"))),
            Set.of(SQUAT),
            Instant.parse("2026-11-09T02:00:00Z"),
            null));

    PlanChangeViewServiceTest() {
        plans.names.put(SQUAT, "Back squat");
    }

    @Test
    void shouldShowAnOwnClientsPlanChange_withTheClientNameTextAndExerciseNames() {
        PlanChangeView view = service.get(TRAINER, stored.getId());

        assertThat(view.planChange()).isSameAs(stored);
        assertThat(view.clientName()).isEqualTo("Ola Nowak");
        assertThat(view.text().title()).isEqualTo("Back squat: cut volume by 10%");
        assertThat(view.exercises()).containsEntry(SQUAT, new ExerciseLabel("Back squat", Measure.REPS));
    }

    @Test
    void shouldWriteInPolish_forATrainerWhoUsesPolish() {
        PlanChangeView view = service.get(POLISH_TRAINER, stored.getId());

        assertThat(view.text().title()).isEqualTo("Back squat PL: obniż objętość o 10%");
        assertThat(view.exercises().get(SQUAT).name()).isEqualTo("Back squat PL");
    }

    @Test
    void shouldAnswerNotFound_forAnotherTrainersClient_asForAMissingPlanChange() {
        assertThatThrownBy(() -> service.get(OTHER_TRAINER, stored.getId()))
                .isInstanceOf(PlanChangeNotFoundException.class);
        assertThatThrownBy(() -> service.get(TRAINER, PlanChangeId.random()))
                .isInstanceOf(PlanChangeNotFoundException.class);
    }

    @Test
    void shouldGiveTheOpenPlanChangeOfAnOwnClient() {
        assertThat(service.openOf(TRAINER, CLIENT).planChange()).isSameAs(stored);
        assertThat(service.openOf(TRAINER, CLIENT).text().reason())
                .isEqualTo("Average RPE was 8.5 and 8.7 in the last 2 weeks, above 8.");
    }

    @Test
    void shouldRefuseTheOpenPlanChange_ofAnotherTrainersClient_orWhenNoneIsOpen() {
        assertThatThrownBy(() -> service.openOf(OTHER_TRAINER, CLIENT)).isInstanceOf(ClientNotFoundException.class);

        planChanges.saved.clear();
        assertThatThrownBy(() -> service.openOf(TRAINER, CLIENT)).isInstanceOf(PlanChangeNotFoundException.class);
    }
}
