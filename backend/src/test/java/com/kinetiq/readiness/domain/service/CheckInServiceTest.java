package com.kinetiq.readiness.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.common.domain.exception.DomainException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.input.DraftSummary;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.readiness.domain.exception.NoAdjustmentException;
import com.kinetiq.readiness.domain.exception.NoSessionTodayException;
import com.kinetiq.readiness.domain.exception.TrainingStartedException;
import com.kinetiq.readiness.domain.input.ChooseTargetsCommand;
import com.kinetiq.readiness.domain.input.SaveCheckInCommand;
import com.kinetiq.readiness.domain.input.TodayCheckIn;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.Cause;
import com.kinetiq.readiness.domain.model.Planned;
import com.kinetiq.readiness.domain.model.Target;
import com.kinetiq.readiness.domain.model.TargetChoice;
import com.kinetiq.readiness.domain.model.CheckIn;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.output.CheckInRepository;
import com.kinetiq.readiness.domain.output.TrainingProgress;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class CheckInServiceTest {

    private static final AccountId CLIENT = AccountId.random();
    private static final AccountId OTHER_CLIENT = AccountId.random();
    // late evening in UTC is already the next day for a client east of it, so the date must come from the plan
    private static final Instant NOW = Instant.parse("2026-11-07T23:30:00Z");
    private static final LocalDate CLIENTS_TODAY = LocalDate.parse("2026-11-08");
    private static final Answers READY = new Answers(4, 2, 1);
    private static final Answers RECOVERY = new Answers(2, 4, 3);
    private static final AccountId TRAINER = AccountId.random();
    private static final Exercise SQUAT = new Exercise(
            ExerciseId.random(),
            TRAINER,
            new LocalizedText("Squat", "Przysiad"),
            Category.LEGS,
            new LocalizedText("", ""),
            Measure.REPS,
            3,
            5,
            120,
            new LocalizedText("", ""));
    private static final Session LOWER_BODY = new Session(
            "Lower body",
            DayOfWeek.SUNDAY,
            List.of(new ExerciseEntry(SQUAT.getId(), 3, 5, new BigDecimal("100.0"), 8, 120)));
    private static final Target PLANNED_SQUAT = new Target(new BigDecimal("100.0"), 5, 8);

    private WeightUnit units = WeightUnit.KG;

    private TodayView.State state = TodayView.State.SESSION;
    private RuntimeException planFailure;
    private Instant now = NOW;
    private final Set<AccountId> withLoggedSet = new HashSet<>();
    private final Map<List<Object>, CheckIn> stored = new HashMap<>();

    private final ViewOwnPlanUseCase plan = new ViewOwnPlanUseCase() {
        @Override
        public OwnPlanView view(AccountId client, Integer week) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void acknowledge(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public DraftSummary draftSummary(AccountId client) {
            throw new UnsupportedOperationException();
        }

        @Override
        public TodayView today(AccountId client) {
            if (planFailure != null) {
                throw planFailure;
            }
            return new TodayView(
                    CLIENTS_TODAY,
                    state,
                    state == TodayView.State.SESSION ? LOWER_BODY : null,
                    Map.of(SQUAT.getId(), SQUAT));
        }
    };

    private final TrainingProgress training = (client, date) -> withLoggedSet.contains(client) && date.equals(CLIENTS_TODAY);

    // keeps copies, so a check-in changed by the service is stored only when it is saved
    private final CheckInRepository checkIns = new CheckInRepository() {
        @Override
        public Optional<CheckIn> findByClientAndDate(AccountId client, LocalDate date) {
            return Optional.ofNullable(stored.get(List.of(client, date))).map(CheckInServiceTest::copy);
        }

        @Override
        public CheckIn save(CheckIn checkIn) {
            stored.put(List.of(checkIn.getClient(), checkIn.getDate()), copy(checkIn));
            return copy(checkIn);
        }

        @Override
        public List<CheckIn> findAllByClientAndDateBetween(AccountId client, LocalDate from, LocalDate to) {
            throw new UnsupportedOperationException();
        }
    };

    private final Clock clock = new Clock() {
        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return now;
        }
    };

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
                    ZoneId.of("Europe/Warsaw"),
                    AccountSettings.startingWith(Language.EN).with(units, null, null),
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

    private final CheckInService service = new CheckInService(plan, training, checkIns, accounts, clock);

    private static CheckIn copy(CheckIn checkIn) {
        return new CheckIn(
                checkIn.getId(),
                checkIn.getClient(),
                checkIn.getDate(),
                checkIn.getAnswers(),
                checkIn.getSavedAt(),
                checkIn.getTargetChoice());
    }

    private TodayCheckIn choose(AccountId client, TargetChoice choice) {
        return service.choose(new ChooseTargetsCommand(client, choice));
    }

    private TodayCheckIn save(AccountId client, Answers answers) {
        return service.save(new SaveCheckInCommand(client, answers));
    }

    @Nested
    class Today {

        @Test
        void shouldBeOpenWithNone_whenTheClientHasNotCheckedIn() {
            TodayCheckIn today = service.today(CLIENT);

            assertThat(today.date()).isEqualTo(CLIENTS_TODAY);
            assertThat(today.open()).isTrue();
            assertThat(today.checkIn()).isNull();
        }

        @Test
        void shouldGiveTheSavedCheckIn_whenTheClientCheckedIn() {
            save(CLIENT, READY);

            TodayCheckIn today = service.today(CLIENT);

            assertThat(today.open()).isTrue();
            assertThat(today.checkIn().getAnswers()).isEqualTo(READY);
            assertThat(today.checkIn().readiness()).isEqualTo(new Readiness(83, Readiness.Band.READY));
        }

        @ParameterizedTest
        @EnumSource(value = TodayView.State.class, names = {"REST", "FINISHED"})
        void shouldNotBeOpen_whenTodayHasNoSession(TodayView.State noSession) {
            state = noSession;

            assertThat(service.today(CLIENT).open()).isFalse();
        }

        @Test
        void shouldNotBeOpenButKeepTheCheckIn_whenASetIsLogged() {
            save(CLIENT, READY);
            withLoggedSet.add(CLIENT);

            TodayCheckIn today = service.today(CLIENT);

            assertThat(today.open()).isFalse();
            assertThat(today.checkIn().getAnswers()).isEqualTo(READY);
        }

        @Test
        void shouldGiveTheLighterTargets_onARecoveryDay() {
            save(CLIENT, RECOVERY);

            TodayCheckIn today = service.today(CLIENT);

            assertThat(today.adjustment().lighter()).containsExactly(new Target(new BigDecimal("90.0"), 5, 7));
            assertThat(today.adjustment().causes()).containsExactly(Cause.SLEEP, Cause.SORENESS);
            assertThat(today.adjustment().inUse()).isTrue();
        }

        @Test
        void shouldGiveNoAdjustment_whenTheClientIsReady() {
            save(CLIENT, READY);

            assertThat(service.today(CLIENT).adjustment()).isNull();
        }

        @Test
        void shouldRoundTheLighterLoadInPounds_whenTheClientUsesPounds() {
            units = WeightUnit.LB;
            save(CLIENT, RECOVERY);

            // 100 kg is 220.5 lb; 198.4 rounds to 198 lb, kept as 89.8 kg
            assertThat(service.today(CLIENT).adjustment().lighter())
                    .containsExactly(new Target(new BigDecimal("89.8"), 5, 7));
        }

        @Test
        void shouldKeepTheAdjustmentButNotBeOpen_onceASetIsLogged() {
            save(CLIENT, RECOVERY);
            withLoggedSet.add(CLIENT);

            TodayCheckIn today = service.today(CLIENT);

            assertThat(today.open()).isFalse();
            assertThat(today.adjustment()).isNotNull();
        }

        @Test
        void shouldNeverGiveAnotherClientsCheckIn() {
            save(OTHER_CLIENT, READY);

            assertThat(service.today(CLIENT).checkIn()).isNull();
        }

        @Test
        void shouldPassTheMissingPlanOn() {
            planFailure = new PlanNotFoundException();

            assertThatThrownBy(() -> service.today(CLIENT)).isInstanceOf(PlanNotFoundException.class);
        }
    }

    @Nested
    class Save {

        @Test
        void shouldStoreOneCheckInForTheClientsDay_andAnswerItsReadiness() {
            TodayCheckIn saved = save(CLIENT, READY);

            assertThat(saved.open()).isTrue();
            assertThat(saved.date()).isEqualTo(CLIENTS_TODAY);
            assertThat(saved.checkIn().getClient()).isEqualTo(CLIENT);
            assertThat(saved.checkIn().getDate()).isEqualTo(CLIENTS_TODAY);
            assertThat(saved.checkIn().getSavedAt()).isEqualTo(NOW);
            assertThat(saved.checkIn().readiness()).isEqualTo(new Readiness(83, Readiness.Band.READY));
            assertThat(stored).hasSize(1);
        }

        @Test
        void shouldReplaceTheAnswers_whenSavedAgainWithOthers() {
            CheckIn first = save(CLIENT, READY).checkIn();
            now = NOW.plusSeconds(60);

            CheckIn changed = save(CLIENT, RECOVERY).checkIn();

            assertThat(stored).hasSize(1);
            assertThat(changed.getId()).isEqualTo(first.getId());
            assertThat(changed.getAnswers()).isEqualTo(RECOVERY);
            assertThat(changed.getSavedAt()).isEqualTo(NOW.plusSeconds(60));
            assertThat(changed.readiness()).isEqualTo(new Readiness(33, Readiness.Band.RECOVERY_DAY));
        }

        @Test
        void shouldAnswerTheLighterTargetsAtOnce_whenTheAnswersMakeARecoveryDay() {
            assertThat(save(CLIENT, RECOVERY).adjustment().lighter())
                    .containsExactly(new Target(new BigDecimal("90.0"), 5, 7));
            assertThat(save(CLIENT, READY).adjustment()).isNull();
        }

        @Test
        void shouldBringTheLighterTargetsBack_whenDifferentAnswersAreSaved() {
            save(CLIENT, RECOVERY);
            choose(CLIENT, TargetChoice.ORIGINAL);

            TodayCheckIn changed = save(CLIENT, new Answers(1, 4, 3));

            assertThat(changed.adjustment().inUse()).isTrue();
        }

        @Test
        void shouldKeepTheOriginalTargets_whenTheSameAnswersAreSavedAgain() {
            save(CLIENT, RECOVERY);
            choose(CLIENT, TargetChoice.ORIGINAL);

            assertThat(save(CLIENT, RECOVERY).adjustment().inUse()).isFalse();
        }

        @Test
        void shouldGiveTheSameResult_whenTheSameAnswersAreSavedTwice() {
            CheckIn first = save(CLIENT, READY).checkIn();
            now = NOW.plusSeconds(60);

            CheckIn again = save(CLIENT, READY).checkIn();

            assertThat(stored).hasSize(1);
            assertThat(again.getId()).isEqualTo(first.getId());
            assertThat(again.getSavedAt()).isEqualTo(NOW);
        }

        @ParameterizedTest
        @EnumSource(value = TodayView.State.class, names = {"REST", "FINISHED"})
        void shouldRefuse_whenTodayHasNoSession(TodayView.State noSession) {
            state = noSession;

            assertThatThrownBy(() -> save(CLIENT, READY))
                    .isInstanceOf(NoSessionTodayException.class)
                    .extracting(error -> ((DomainException) error).code())
                    .isEqualTo("check-in.no-session-today");
            assertThat(stored).isEmpty();
        }

        @Test
        void shouldRefuseANewCheckIn_whenASetIsLogged() {
            withLoggedSet.add(CLIENT);

            assertThatThrownBy(() -> save(CLIENT, READY))
                    .isInstanceOf(TrainingStartedException.class)
                    .extracting(error -> ((DomainException) error).code())
                    .isEqualTo("check-in.training-started");
            assertThat(stored).isEmpty();
        }

        @Test
        void shouldRefuseAChangeAndKeepTheAnswers_whenASetIsLogged() {
            save(CLIENT, READY);
            withLoggedSet.add(CLIENT);

            assertThatThrownBy(() -> save(CLIENT, RECOVERY)).isInstanceOf(TrainingStartedException.class);
            assertThat(service.today(CLIENT).checkIn().getAnswers()).isEqualTo(READY);
        }

        @Test
        void shouldNotTouchAnotherClientsCheckIn() {
            save(OTHER_CLIENT, READY);

            save(CLIENT, RECOVERY);

            assertThat(stored).hasSize(2);
            assertThat(service.today(CLIENT).checkIn().getAnswers()).isEqualTo(RECOVERY);
            assertThat(stored.get(List.of(OTHER_CLIENT, CLIENTS_TODAY)).getAnswers()).isEqualTo(READY);
        }

        @Test
        void shouldPassTheMissingPlanOn_andStoreNothing() {
            planFailure = new PlanNotFoundException();

            assertThatThrownBy(() -> save(CLIENT, READY)).isInstanceOf(PlanNotFoundException.class);
            assertThat(stored).isEmpty();
        }
    }

    @Nested
    class Choose {

        @Test
        void shouldStoreTheOriginalTargets_andTheLighterOnesAgain() {
            save(CLIENT, RECOVERY);

            assertThat(choose(CLIENT, TargetChoice.ORIGINAL).adjustment().inUse()).isFalse();
            assertThat(service.today(CLIENT).adjustment().inUse()).isFalse();
            assertThat(choose(CLIENT, TargetChoice.LIGHTER).adjustment().inUse()).isTrue();
            assertThat(service.today(CLIENT).adjustment().inUse()).isTrue();
        }

        @Test
        void shouldGiveTheSameResult_whenTheChoiceInUseIsSentAgain() {
            save(CLIENT, RECOVERY);
            choose(CLIENT, TargetChoice.ORIGINAL);

            assertThat(choose(CLIENT, TargetChoice.ORIGINAL).adjustment().inUse()).isFalse();
            assertThat(stored).hasSize(1);
        }

        @Test
        void shouldRefuse_whenThereIsNoCheckInToday() {
            assertThatThrownBy(() -> choose(CLIENT, TargetChoice.ORIGINAL))
                    .isInstanceOf(NoAdjustmentException.class)
                    .extracting(error -> ((DomainException) error).code())
                    .isEqualTo("check-in.no-adjustment");
        }

        @Test
        void shouldRefuse_whenTodaysTargetsAreAsPlanned() {
            save(CLIENT, READY);

            assertThatThrownBy(() -> choose(CLIENT, TargetChoice.ORIGINAL)).isInstanceOf(NoAdjustmentException.class);
            assertThat(stored.get(List.of(CLIENT, CLIENTS_TODAY)).getTargetChoice()).isEqualTo(TargetChoice.LIGHTER);
        }

        @Test
        void shouldRefuseAndKeepTheChoice_whenASetIsLogged() {
            save(CLIENT, RECOVERY);
            withLoggedSet.add(CLIENT);

            assertThatThrownBy(() -> choose(CLIENT, TargetChoice.ORIGINAL))
                    .isInstanceOf(TrainingStartedException.class);
            assertThat(service.today(CLIENT).adjustment().inUse()).isTrue();
        }

        @ParameterizedTest
        @EnumSource(value = TodayView.State.class, names = {"REST", "FINISHED"})
        void shouldRefuse_whenTodayHasNoSession(TodayView.State noSession) {
            state = noSession;

            assertThatThrownBy(() -> choose(CLIENT, TargetChoice.ORIGINAL))
                    .isInstanceOf(NoSessionTodayException.class);
        }

        @Test
        void shouldNotChangeAnotherClientsChoice() {
            save(OTHER_CLIENT, RECOVERY);
            save(CLIENT, RECOVERY);

            choose(CLIENT, TargetChoice.ORIGINAL);

            assertThat(stored.get(List.of(OTHER_CLIENT, CLIENTS_TODAY)).getTargetChoice())
                    .isEqualTo(TargetChoice.LIGHTER);
        }
    }

    @Nested
    class AdjustmentOn {

        private final List<Planned> session = List.of(new Planned(PLANNED_SQUAT, Measure.REPS));

        @Test
        void shouldAdjustTheGivenEntries_byTheCheckInOfThatDate() {
            save(CLIENT, RECOVERY);

            assertThat(service.adjustmentOn(CLIENT, CLIENTS_TODAY, session).orElseThrow().inUse(0, PLANNED_SQUAT))
                    .isEqualTo(new Target(new BigDecimal("90.0"), 5, 7));
        }

        @Test
        void shouldGiveNone_withoutACheckInOnThatDate() {
            save(CLIENT, RECOVERY);

            assertThat(service.adjustmentOn(CLIENT, CLIENTS_TODAY.minusDays(1), session)).isEmpty();
            assertThat(service.adjustmentOn(OTHER_CLIENT, CLIENTS_TODAY, session)).isEmpty();
        }
    }
}
