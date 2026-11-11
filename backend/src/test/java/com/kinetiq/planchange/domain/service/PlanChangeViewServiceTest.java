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
import com.kinetiq.planchange.domain.input.PlanChangeApprovalView;
import com.kinetiq.planchange.domain.input.PlanChangeView;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.Decision;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.output.PlanChangeApprovals;
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

    private final FakePlanChangeApprovals approvals = new FakePlanChangeApprovals();
    private final MutableClock clock = new MutableClock("2026-11-12T10:00:00Z");

    private final PlanChangeViewService service =
            new PlanChangeViewService(clients, accounts, planChanges, plans, approvals, clock);

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
            null,
            null,
            null,
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

    private PlanChange declinedAt(String endedAt) {
        return planChanges.save(new PlanChange(
                PlanChangeId.random(),
                CLIENT,
                Rule.HIGH_RPE,
                PlanChangeState.DECLINED,
                1,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                LocalDate.parse("2026-11-16"),
                stored.getContent(),
                stored.getFigures(),
                Set.of(SQUAT),
                Instant.parse("2026-10-01T02:00:00Z"),
                Instant.parse(endedAt),
                null,
                null,
                new Decision(Side.CLIENT, DeclineReason.OTHER, null)));
    }

    @Test
    void shouldListTheApprovals_inTheOrderGiven_withClientNamesAndTheTrainersLanguage() {
        PlanChange declined = declinedAt("2026-11-10T09:00:00Z");
        approvals.entries.add(new PlanChangeApprovals.Entry(stored.getId(), "Ola Nowak"));
        approvals.entries.add(new PlanChangeApprovals.Entry(declined.getId(), "Ola Nowak"));

        List<PlanChangeApprovalView> views = service.approvals(POLISH_TRAINER);

        assertThat(views).extracting(view -> view.planChange().getId()).containsExactly(stored.getId(), declined.getId());
        assertThat(views).extracting(PlanChangeApprovalView::clientName).containsOnly("Ola Nowak");
        assertThat(views.getFirst().text().title()).isEqualTo("Back squat PL: obniż objętość o 10%");
        assertThat(approvals.askedBy).isEqualTo(POLISH_TRAINER);
    }

    @Test
    void shouldAskForThoseEndedInTheLast30Days_countedFromNow() {
        service.approvals(TRAINER);

        assertThat(approvals.askedSince).isEqualTo(Instant.parse("2026-10-13T10:00:00Z"));
    }

    @Test
    void shouldListNothing_whenThereIsNothingToShow() {
        assertThat(service.approvals(TRAINER)).isEmpty();
    }

    @Test
    void shouldListTheLast5EndedOfAnOwnClient_latestFirst() {
        for (int day = 1; day <= 7; day++) {
            declinedAt("2026-11-0" + day + "T09:00:00Z");
        }

        List<PlanChangeApprovalView> ended = service.ended(TRAINER, CLIENT);

        assertThat(ended).hasSize(5);
        assertThat(ended).extracting(view -> view.planChange().getEndedAt().orElseThrow())
                .containsExactly(
                        Instant.parse("2026-11-07T09:00:00Z"),
                        Instant.parse("2026-11-06T09:00:00Z"),
                        Instant.parse("2026-11-05T09:00:00Z"),
                        Instant.parse("2026-11-04T09:00:00Z"),
                        Instant.parse("2026-11-03T09:00:00Z"));
        assertThat(ended).extracting(PlanChangeApprovalView::clientName).containsOnly("Ola Nowak");
    }

    @Test
    void shouldNotListTheOpenOne_amongTheEnded() {
        assertThat(service.ended(TRAINER, CLIENT)).isEmpty();
    }

    @Test
    void shouldRefuseTheEnded_ofAnotherTrainersClient() {
        declinedAt("2026-11-03T09:00:00Z");

        assertThatThrownBy(() -> service.ended(OTHER_TRAINER, CLIENT)).isInstanceOf(ClientNotFoundException.class);
    }
}
