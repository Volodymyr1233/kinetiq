package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanWeekOutOfRangeException;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanViewServiceTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final AccountId OTHER_CLIENT = AccountId.random();
    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final ZoneId LOS_ANGELES = ZoneId.of("America/Los_Angeles");
    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-09-28");
    // Wednesday of plan week 6
    private static final Instant NOW = Instant.parse("2026-11-04T10:00:00Z");

    private static final Exercise OWN_EXERCISE = new Exercise(
            ExerciseId.random(),
            TRAINER,
            new LocalizedText("Squat", "Przysiad"),
            Category.LEGS,
            new LocalizedText("", ""),
            Measure.REPS,
            3,
            10,
            90,
            new LocalizedText("", ""));

    private final InMemoryPlanRepository plans = new InMemoryPlanRepository();
    private ZoneId clientZone = WARSAW;
    private AccountId libraryOwner;

    private final CurrentAccountUseCase accounts = new CurrentAccountUseCase() {
        @Override
        public Account find(AccountId id) {
            return Account.restore(
                    id,
                    "Ola",
                    new Email("ola@example.com"),
                    "hash",
                    Role.CLIENT,
                    TRAINER,
                    clientZone,
                    AccountSettings.startingWith(Language.EN),
                    true,
                    null,
                    null,
                    null,
                    NOW);
        }

        @Override
        public void changeTimeZone(AccountId id, ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Account update(AccountId id, UpdateAccountCommand command) {
            throw new UnsupportedOperationException();
        }
    };

    private final ListExercisesUseCase library = new ListExercisesUseCase() {
        @Override
        public ExerciseListing list(ListExercisesQuery query) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<Exercise> library(AccountId trainer) {
            libraryOwner = trainer;
            return List.of(OWN_EXERCISE);
        }
    };

    private final PlanViewService service = serviceAt(NOW);

    private PlanViewService serviceAt(Instant now) {
        return new PlanViewService(accounts, library, plans, Clock.fixed(now, ZoneOffset.UTC));
    }

    private static List<Session> sessions() {
        ExerciseEntry entry = new ExerciseEntry(OWN_EXERCISE.getId(), 3, 8, null, 8, 90);
        return List.of(
                new Session("Upper", DayOfWeek.MONDAY, List.of(entry)),
                new Session("Lower", DayOfWeek.THURSDAY, List.of(entry)));
    }

    private void storePublished(int weeks, LocalDate firstWeek) {
        plans.byClient.put(
                CLIENT, new Plan(PlanId.random(), CLIENT, Goal.GET_STRONGER, weeks, firstWeek, sessions(), 0));
    }

    private void storeDraft() {
        plans.byClient.put(CLIENT, Plan.draft(CLIENT, Goal.GET_STRONGER, 12, sessions()));
    }

    @Nested
    class View {

        @Test
        void shouldAnswerLikeNoPlan_whenTheDraftWasBuiltAutomatically() {
            plans.byClient.put(CLIENT, Plan.built(CLIENT, Goal.GET_STRONGER, 12, sessions()));

            assertThatThrownBy(() -> service.view(CLIENT, null)).isInstanceOf(PlanNotFoundException.class);
            assertThatThrownBy(() -> service.acknowledge(CLIENT)).isInstanceOf(PlanNotFoundException.class);
        }

        @Test
        void shouldAnswerLikeNoPlan_whenTheDraftFailed() {
            plans.byClient.put(CLIENT, Plan.failed(CLIENT, Goal.GET_STRONGER, 12));

            assertThatThrownBy(() -> service.view(CLIENT, null)).isInstanceOf(PlanNotFoundException.class);
            assertThatThrownBy(() -> service.acknowledge(CLIENT)).isInstanceOf(PlanNotFoundException.class);
        }

        @Test
        void shouldShowTheCurrentWeekWithItsMonday_whenNoWeekIsAsked() {
            storePublished(12, FIRST_WEEK);

            OwnPlanView view = service.view(CLIENT, null);

            assertThat(view.currentWeek()).isEqualTo(6);
            assertThat(view.finished()).isFalse();
            assertThat(view.week()).isEqualTo(new OwnPlanView.Week(6, LocalDate.parse("2026-11-02")));
            assertThat(view.plan().getSessions()).extracting(Session::name).containsExactly("Upper", "Lower");
        }

        @Test
        void shouldShowAnotherWeekOfThePlan_whenItIsAsked() {
            storePublished(12, FIRST_WEEK);

            assertThat(service.view(CLIENT, 1).week()).isEqualTo(new OwnPlanView.Week(1, FIRST_WEEK));
            assertThat(service.view(CLIENT, 12).week())
                    .isEqualTo(new OwnPlanView.Week(12, LocalDate.parse("2026-12-14")));
        }

        @Test
        void shouldGiveTheNamesOfTheClientsTrainersLibrary() {
            storePublished(12, FIRST_WEEK);

            OwnPlanView view = service.view(CLIENT, null);

            assertThat(libraryOwner).isEqualTo(TRAINER);
            assertThat(view.library()).containsKey(OWN_EXERCISE.getId());
        }

        @Test
        void shouldRefuseAWeekThePlanDoesNotHave() {
            storePublished(12, FIRST_WEEK);

            assertThatThrownBy(() -> service.view(CLIENT, 0)).isInstanceOf(PlanWeekOutOfRangeException.class);
            assertThatThrownBy(() -> service.view(CLIENT, -3)).isInstanceOf(PlanWeekOutOfRangeException.class);
            assertThatThrownBy(() -> service.view(CLIENT, 13)).isInstanceOf(PlanWeekOutOfRangeException.class);
        }

        @Test
        void shouldAnswerFinishedWithNoWeek_whenThePlanIsPastItsLastWeek() {
            storePublished(4, FIRST_WEEK);

            OwnPlanView view = service.view(CLIENT, null);

            assertThat(view.finished()).isTrue();
            assertThat(view.currentWeek()).isEqualTo(6);
            assertThat(view.week()).isNull();
            assertThat(view.plan().getSessions()).hasSize(2);
        }

        @Test
        void shouldRefuseAnyWeek_whenThePlanHasFinished() {
            storePublished(4, FIRST_WEEK);

            assertThatThrownBy(() -> service.view(CLIENT, 4)).isInstanceOf(PlanWeekOutOfRangeException.class);
        }

        @Test
        void shouldTreatTheLastWeekAsAnOrdinaryWeek() {
            storePublished(6, FIRST_WEEK);

            OwnPlanView view = service.view(CLIENT, null);

            assertThat(view.finished()).isFalse();
            assertThat(view.week().number()).isEqualTo(6);
        }

        @Test
        void shouldCountWeeksInTheClientsTimeZone_whenSundayNightIsAlreadyMondayElsewhere() {
            storePublished(12, FIRST_WEEK);
            // Sunday 8 November 20:30 in Los Angeles is already Monday 9 November in UTC and in Warsaw
            Instant sundayNight = Instant.parse("2026-11-09T04:30:00Z");

            clientZone = LOS_ANGELES;
            assertThat(serviceAt(sundayNight).view(CLIENT, null).currentWeek()).isEqualTo(6);

            clientZone = WARSAW;
            assertThat(serviceAt(sundayNight).view(CLIENT, null).currentWeek()).isEqualTo(7);
        }

        @Test
        void shouldReportWhetherTheClientHasSeenThePlan() {
            storePublished(12, FIRST_WEEK);
            assertThat(service.view(CLIENT, null).seen()).isFalse();

            service.acknowledge(CLIENT);

            assertThat(service.view(CLIENT, null).seen()).isTrue();
        }

        @Test
        void shouldAnswerNotFoundAlike_whenThePlanIsADraftOrThereIsNone() {
            storeDraft();

            assertThatThrownBy(() -> service.view(CLIENT, null)).isExactlyInstanceOf(PlanNotFoundException.class);
            assertThatThrownBy(() -> service.view(OTHER_CLIENT, null))
                    .isExactlyInstanceOf(PlanNotFoundException.class);
        }

        @Test
        void shouldNotLetAWeekAskedForRevealADraft() {
            storeDraft();

            assertThatThrownBy(() -> service.view(CLIENT, 99)).isExactlyInstanceOf(PlanNotFoundException.class);
        }
    }

    @Nested
    class Today {

        // Thursday of plan week 6
        private static final Instant THURSDAY_NOON = Instant.parse("2026-11-05T11:00:00Z");

        @Test
        void shouldGiveTheSessionOnThisWeekday_withTheNamesOfTheTrainersLibrary() {
            storePublished(12, FIRST_WEEK);

            TodayView today = serviceAt(THURSDAY_NOON).today(CLIENT);

            assertThat(today.state()).isEqualTo(TodayView.State.SESSION);
            assertThat(today.date()).isEqualTo(LocalDate.parse("2026-11-05"));
            assertThat(today.session().name()).isEqualTo("Lower");
            assertThat(libraryOwner).isEqualTo(TRAINER);
            assertThat(today.library()).containsKey(OWN_EXERCISE.getId());
        }

        @Test
        void shouldAnswerRest_whenThereIsNoSessionOnThisWeekday() {
            storePublished(12, FIRST_WEEK);

            TodayView today = service.today(CLIENT);

            assertThat(today.state()).isEqualTo(TodayView.State.REST);
            assertThat(today.session()).isNull();
        }

        @Test
        void shouldAnswerFinished_whenThePlanIsPastItsLastWeek() {
            storePublished(4, FIRST_WEEK);

            // a Thursday, which would be a session day while the plan ran
            TodayView today = serviceAt(THURSDAY_NOON).today(CLIENT);

            assertThat(today.state()).isEqualTo(TodayView.State.FINISHED);
            assertThat(today.session()).isNull();
        }

        @Test
        void shouldEndThePlanAfterItsLastSunday() {
            storePublished(5, FIRST_WEEK);
            // Sunday 1 November closes plan week 5; Thursday of week 5 is 29 October
            assertThat(serviceAt(Instant.parse("2026-10-29T11:00:00Z")).today(CLIENT).state())
                    .isEqualTo(TodayView.State.SESSION);
            assertThat(serviceAt(Instant.parse("2026-11-01T11:00:00Z")).today(CLIENT).state())
                    .isEqualTo(TodayView.State.REST);
            assertThat(serviceAt(Instant.parse("2026-11-02T11:00:00Z")).today(CLIENT).state())
                    .isEqualTo(TodayView.State.FINISHED);
        }

        @Test
        void shouldUseTheClientsLocalDate_whenMidnightHasPassedElsewhere() {
            storePublished(12, FIRST_WEEK);
            // Wednesday 15:30 in Los Angeles is already Thursday 00:30 in Warsaw
            Instant crossing = Instant.parse("2026-11-04T23:30:00Z");

            clientZone = LOS_ANGELES;
            TodayView losAngeles = serviceAt(crossing).today(CLIENT);
            assertThat(losAngeles.date()).isEqualTo(LocalDate.parse("2026-11-04"));
            assertThat(losAngeles.state()).isEqualTo(TodayView.State.REST);

            clientZone = WARSAW;
            TodayView warsaw = serviceAt(crossing).today(CLIENT);
            assertThat(warsaw.date()).isEqualTo(LocalDate.parse("2026-11-05"));
            assertThat(warsaw.state()).isEqualTo(TodayView.State.SESSION);
            assertThat(warsaw.session().name()).isEqualTo("Lower");
        }

        @Test
        void shouldAnswerNotFoundAlike_whenThePlanIsADraftOrThereIsNone() {
            storeDraft();

            assertThatThrownBy(() -> service.today(CLIENT)).isExactlyInstanceOf(PlanNotFoundException.class);
            assertThatThrownBy(() -> service.today(OTHER_CLIENT)).isExactlyInstanceOf(PlanNotFoundException.class);
        }
    }

    @Nested
    class Acknowledge {

        @Test
        void shouldRefuseADraftAndAClientWithNoPlan() {
            storeDraft();

            assertThatThrownBy(() -> service.acknowledge(CLIENT)).isExactlyInstanceOf(PlanNotFoundException.class);
            assertThatThrownBy(() -> service.acknowledge(OTHER_CLIENT))
                    .isExactlyInstanceOf(PlanNotFoundException.class);
            assertThat(plans.seen).isEmpty();
        }

        @Test
        void shouldBeSafeToRepeat() {
            storePublished(12, FIRST_WEEK);

            service.acknowledge(CLIENT);
            service.acknowledge(CLIENT);

            assertThat(plans.seen).containsExactly(CLIENT);
        }
    }
}
