package com.kinetiq.progress.domain.service;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.input.DraftSummary;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.TodayView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.progress.domain.input.Progress;
import com.kinetiq.progress.domain.input.Progress.ExerciseProgress;
import com.kinetiq.progress.domain.input.Progress.TopSetChange;
import com.kinetiq.progress.domain.input.Rewards;
import com.kinetiq.progress.domain.model.LoggedExercise;
import com.kinetiq.progress.domain.model.Period;
import com.kinetiq.progress.domain.model.Pr;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.progress.domain.model.TrainingWeek;
import com.kinetiq.progress.domain.output.TrainingLog;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ProgressViewServiceTest {

    private static final AccountId TRAINER = AccountId.random();
    private static final AccountId OTHER_TRAINER = AccountId.random();
    private static final AccountId CLIENT = AccountId.random();
    private static final ZoneId ZONE = ZoneId.of("Europe/Warsaw");
    private static final LocalDate FIRST_MONDAY = LocalDate.parse("2026-09-07");
    private static final Period FOUR_WEEKS = new Period(4);
    private static final Exercise SQUAT = exercise("Squat");
    private static final Exercise ROW = exercise("Row");
    private static final Exercise PRESS = exercise("Press");
    private static final Exercise PULL_UP = exercise("Pull-up");
    private static final Exercise DIP = exercise("Dip");
    private static final Map<ExerciseId, Exercise> LIBRARY = Stream.of(SQUAT, ROW, PRESS, PULL_UP, DIP)
            .collect(Collectors.toMap(Exercise::getId, exercise -> exercise));

    private Plan plan = publishedPlan(12, FIRST_MONDAY);
    private Instant now = wednesday(4);
    private final List<LoggedExercise> rows = new ArrayList<>();
    private final Set<LocalDate> finished = new HashSet<>();
    private final List<String> reads = new ArrayList<>();

    private static Exercise exercise(String name) {
        return new Exercise(
                ExerciseId.random(),
                TRAINER,
                new LocalizedText(name, name),
                Category.LEGS,
                new LocalizedText("", ""),
                Measure.REPS,
                3,
                5,
                60,
                new LocalizedText("", ""));
    }

    // sessions on Monday, Wednesday and Friday
    private static Plan publishedPlan(int weeks, LocalDate publishedOn) {
        return new Plan(
                PlanId.random(),
                CLIENT,
                Goal.GET_STRONGER,
                weeks,
                FIRST_MONDAY,
                publishedOn,
                Stream.of(MONDAY, WEDNESDAY, FRIDAY)
                        .map(day -> new Session("Session", day, List.of()))
                        .toList(),
                0,
                null);
    }

    private static LocalDate day(int week, DayOfWeek day) {
        return FIRST_MONDAY.plusWeeks(week - 1L).plusDays(day.getValue() - 1L);
    }

    // 11:00 in Warsaw
    private static Instant wednesday(int week) {
        return day(week, WEDNESDAY).atTime(LocalTime.of(10, 0)).toInstant(ZoneOffset.UTC);
    }

    // one set of the exercise in the session log of that day; loadKg is null for a set without a load
    private void logged(int week, DayOfWeek day, Exercise exercise, String loadKg, int amount, int rpe) {
        rows.add(new LoggedExercise(
                day(week, day),
                exercise.getId(),
                new TopSet(loadKg == null ? null : new BigDecimal(loadKg), amount, rpe),
                rpe,
                1));
    }

    private void finish(int week, DayOfWeek... days) {
        Stream.of(days).forEach(day -> finished.add(day(week, day)));
    }

    private void finishWholeWeeks(int from, int to) {
        for (int week = from; week <= to; week++) {
            finish(week, MONDAY, WEDNESDAY, FRIDAY);
        }
    }

    private static ExerciseProgress progressOf(Progress progress, Exercise exercise) {
        return progress.getExercises().stream()
                .filter(candidate -> candidate.getExercise().equals(exercise))
                .findFirst()
                .orElseThrow();
    }

    private final ViewClientsUseCase clients = new ViewClientsUseCase() {
        @Override
        public List<ClientSummary> listClients(AccountId trainer) {
            throw new UnsupportedOperationException();
        }

        @Override
        public ClientSummary getClient(AccountId trainer, AccountId client) {
            if (!trainer.equals(TRAINER) || !client.equals(CLIENT)) {
                throw new ClientNotFoundException();
            }
            return new ClientSummary(CLIENT, "Ola", Goal.GET_STRONGER, PlanState.PUBLISHED, ZONE);
        }
    };

    private final ViewOwnPlanUseCase plans = new ViewOwnPlanUseCase() {
        @Override
        public OwnPlanView view(AccountId client, Integer week) {
            reads.add("plan");
            if (plan == null) {
                throw new PlanNotFoundException();
            }
            int current = plan.currentWeek(now.atZone(ZONE).toLocalDate()).orElseThrow();
            boolean done = current > plan.getWeeks();
            OwnPlanView.Week shown = done ? null : new OwnPlanView.Week(current, FIRST_MONDAY.plusWeeks(current - 1L));
            return new OwnPlanView(plan, shown, current, done, true, LIBRARY);
        }

        @Override
        public TodayView today(AccountId client) {
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
                    ZONE,
                    AccountSettings.startingWith(Language.EN),
                    true,
                    null,
                    null,
                    null,
                    FIRST_MONDAY.atStartOfDay().toInstant(ZoneOffset.UTC));
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

    // the rows come oldest first, as the port promises
    private final TrainingLog trainingLog = new TrainingLog() {
        @Override
        public List<LoggedExercise> loggedExercises(AccountId client) {
            reads.add("logs");
            return rows.stream().sorted(Comparator.comparing(LoggedExercise::date)).toList();
        }

        @Override
        public Set<LocalDate> finishedDays(AccountId client) {
            reads.add("logs");
            return Set.copyOf(finished);
        }
    };

    private Progress own(Period period) {
        return new ProgressViewService(clients, plans, accounts, trainingLog, Clock.fixed(now, ZoneOffset.UTC))
                .own(CLIENT, period);
    }

    private Rewards rewards(LocalDate date) {
        return new ProgressViewService(clients, plans, accounts, trainingLog, Clock.fixed(now, ZoneOffset.UTC))
                .rewards(CLIENT, date);
    }

    @Nested
    class ThePeriod {

        @Test
        void shouldGiveWeeks1To3_withTheThirdRunning_whenEightWeeksAreAskedInWeek3() {
            now = wednesday(3);

            Progress progress = own(new Period(8));

            assertThat(progress.getWeeks()).extracting(TrainingWeek::number).containsExactly(1, 2, 3);
            assertThat(progress.getWeeks()).extracting(TrainingWeek::running).containsExactly(false, false, true);
            assertThat(progress.getWeek()).isEqualTo(3);
            assertThat(progress.isFinished()).isFalse();
        }

        @Test
        void shouldGiveWeeks7To10_withTheLastRunning_whenFourWeeksAreAskedInWeek10() {
            now = wednesday(10);

            Progress progress = own(FOUR_WEEKS);

            assertThat(progress.getWeeks()).extracting(TrainingWeek::number).containsExactly(7, 8, 9, 10);
            assertThat(progress.getWeeks())
                    .extracting(TrainingWeek::running)
                    .containsExactly(false, false, false, true);
        }

        @Test
        void shouldGiveItsLastFourWeeks_noneRunning_whenASixWeekPlanHasFinished() {
            plan = publishedPlan(6, FIRST_MONDAY);
            now = wednesday(8);

            Progress progress = own(FOUR_WEEKS);

            assertThat(progress.getWeeks()).extracting(TrainingWeek::number).containsExactly(3, 4, 5, 6);
            assertThat(progress.getWeeks()).extracting(TrainingWeek::running).containsOnly(false);
            assertThat(progress.getWeek()).isEqualTo(6);
            assertThat(progress.isFinished()).isTrue();
        }
    }

    @Nested
    class Sums {

        @Test
        void shouldCountTheSessionsAndAverageEverySetOfThePeriod() {
            finishWholeWeeks(1, 2);
            finish(3, MONDAY, WEDNESDAY);
            finish(4, MONDAY);
            logged(1, MONDAY, SQUAT, "100.0", 5, 7);
            logged(2, MONDAY, SQUAT, "100.0", 5, 8);
            logged(3, MONDAY, SQUAT, "100.0", 5, 9);
            logged(4, MONDAY, SQUAT, "100.0", 5, 9);

            Progress progress = own(FOUR_WEEKS);

            // the rest of week 4 is still planned
            assertThat(progress.getSessionsDone()).isEqualTo(9);
            assertThat(progress.getSessionsPlanned()).isEqualTo(12);
            // 33 / 4 = 8.25, which half up makes 8.3
            assertThat(progress.getAverageRpe()).contains(new BigDecimal("8.3"));
        }

        @Test
        void shouldHaveNoMeanRpe_whenThePeriodHasNoSet() {
            finish(4, MONDAY);

            assertThat(own(FOUR_WEEKS).getAverageRpe()).isEmpty();
        }
    }

    @Nested
    class Comparisons {

        @Test
        void shouldCompareWeek9WithWeek8_whileWeek10Runs() {
            now = wednesday(10);
            finishWholeWeeks(8, 8);
            finish(9, MONDAY, WEDNESDAY);
            finish(10, MONDAY);
            logged(8, MONDAY, SQUAT, "100.0", 5, 7);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(10, MONDAY, SQUAT, "100.0", 5, 10);

            Progress progress = own(FOUR_WEEKS);

            assertThat(progress.getSessionsDoneChange()).contains(-1);
            assertThat(progress.getAverageRpeChange()).contains(new BigDecimal("1.0"));
        }

        @Test
        void shouldCompareItsLastTwoWeeks_whenThePlanHasFinished() {
            plan = publishedPlan(6, FIRST_MONDAY);
            now = wednesday(8);
            finishWholeWeeks(5, 5);
            finish(6, MONDAY, WEDNESDAY);

            assertThat(own(FOUR_WEEKS).getSessionsDoneChange()).contains(-1);
        }

        @Test
        void shouldCompareNothing_inPlanWeek1() {
            now = wednesday(1);
            finish(1, MONDAY);
            logged(1, MONDAY, SQUAT, "100.0", 5, 8);

            Progress progress = own(FOUR_WEEKS);

            assertThat(progress.getSessionsDoneChange()).isEmpty();
            assertThat(progress.getAverageRpeChange()).isEmpty();
            assertThat(progressOf(progress, SQUAT).getTopSetChange()).isEmpty();
        }

        @Test
        void shouldNotCompareTheRpe_whenOneOfTheWeeksHasNoSet() {
            now = wednesday(10);
            finish(8, MONDAY);
            finish(9, MONDAY);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);

            Progress progress = own(FOUR_WEEKS);

            assertThat(progress.getAverageRpeChange()).isEmpty();
            assertThat(progress.getSessionsDoneChange()).contains(0);
        }

        @Test
        void shouldCompareTopSetsInKilogramsOrInAmount_butNotALoadWithNone() {
            now = wednesday(10);
            logged(8, MONDAY, SQUAT, "100.0", 5, 8);
            logged(9, MONDAY, SQUAT, "102.5", 5, 8);
            logged(8, MONDAY, PULL_UP, null, 8, 8);
            logged(9, MONDAY, PULL_UP, null, 10, 8);
            logged(8, MONDAY, DIP, "20.0", 8, 8);
            logged(9, MONDAY, DIP, null, 12, 8);

            Progress progress = own(FOUR_WEEKS);

            assertThat(progressOf(progress, SQUAT).getTopSetChange())
                    .contains(new TopSetChange(new BigDecimal("2.5"), null));
            assertThat(progressOf(progress, PULL_UP).getTopSetChange()).contains(new TopSetChange(null, 2));
            assertThat(progressOf(progress, DIP).getTopSetChange()).isEmpty();
        }
    }

    @Nested
    class WeeksInARow {

        @Test
        void shouldCountThreeWholeWeeks() {
            finishWholeWeeks(1, 3);

            assertThat(own(FOUR_WEEKS).getWeeksInARow()).isEqualTo(3);
        }

        @Test
        void shouldCountAFirstWeekShortenedByThePublishDay_whenItsOneSessionIsDone() {
            plan = publishedPlan(12, day(1, FRIDAY));
            now = wednesday(3);
            finish(1, FRIDAY);
            finish(2, MONDAY, WEDNESDAY);

            assertThat(own(FOUR_WEEKS).getWeeksInARow()).isEqualTo(2);
        }

        @Test
        void shouldEndAtAWeekWithOneOfThreeDone() {
            finishWholeWeeks(1, 1);
            finish(2, MONDAY);
            finish(3, MONDAY, WEDNESDAY);

            assertThat(own(FOUR_WEEKS).getWeeksInARow()).isEqualTo(1);
        }

        @Test
        void shouldNotCountAFirstWeekWithoutAPlannedSession() {
            plan = publishedPlan(12, day(1, SATURDAY));
            now = wednesday(2);

            assertThat(own(FOUR_WEEKS).getWeeksInARow()).isZero();
        }

        @Test
        void shouldAddTheRunningWeek_onlyOnceItHasTwoDone() {
            now = wednesday(2);
            finishWholeWeeks(1, 1);
            finish(2, MONDAY);

            assertThat(own(FOUR_WEEKS).getWeeksInARow()).isEqualTo(1);

            finish(2, WEDNESDAY);

            assertThat(own(FOUR_WEEKS).getWeeksInARow()).isEqualTo(2);
        }
    }

    @Nested
    class RewardsOfADate {

        @Test
        void shouldGiveThePrsOfTheDateAsked_notThoseOfALaterSession() {
            now = wednesday(10);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(10, MONDAY, SQUAT, "102.5", 5, 8);
            logged(10, WEDNESDAY, SQUAT, "105.0", 5, 9);

            assertThat(rewards(day(10, MONDAY)).prs())
                    .containsExactly(new Pr(SQUAT.getId(), new TopSet(new BigDecimal("102.5"), 5, 8)));
        }

        @Test
        void shouldGiveTheStreakOfTheProgress_whenAWeekBreaksIt() {
            finishWholeWeeks(1, 1);
            finish(2, MONDAY);
            finish(3, MONDAY, WEDNESDAY);

            assertThat(rewards(day(3, WEDNESDAY)).weeksInARow())
                    .isEqualTo(own(FOUR_WEEKS).getWeeksInARow())
                    .isEqualTo(1);
        }

        @Test
        void shouldAddTheRunningWeek_onlyOnceItHasTwoDone() {
            now = wednesday(2);
            finishWholeWeeks(1, 1);
            finish(2, MONDAY);

            assertThat(rewards(day(2, MONDAY)).weeksInARow()).isEqualTo(1);

            finish(2, WEDNESDAY);

            assertThat(rewards(day(2, WEDNESDAY)).weeksInARow()).isEqualTo(2);
        }
    }

    @Nested
    class PrToday {

        @Test
        void shouldBeOne_whenTodaysSetBeatsEveryEarlierOne() {
            now = wednesday(10);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(10, WEDNESDAY, SQUAT, "102.5", 5, 9);

            assertThat(progressOf(own(FOUR_WEEKS), SQUAT).isPrToday()).isTrue();
        }

        @Test
        void shouldNotBeOne_whenTodaysSetOnlyEqualsAnEarlierOne() {
            now = wednesday(10);
            logged(9, MONDAY, SQUAT, "102.5", 5, 8);
            logged(10, WEDNESDAY, SQUAT, "102.5", 5, 9);

            assertThat(progressOf(own(FOUR_WEEKS), SQUAT).isPrToday()).isFalse();
        }

        @Test
        void shouldNotBeOne_whenTheHeavierSetWasYesterday() {
            now = wednesday(10);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(10, TUESDAY, SQUAT, "102.5", 5, 9);

            assertThat(progressOf(own(FOUR_WEEKS), SQUAT).isPrToday()).isFalse();
        }

        @Test
        void shouldTakeTheClientsDate_whenItIsAlreadyWednesdayInTheirZone() {
            // 23:30 UTC on Tuesday is 00:30 on Wednesday in Warsaw
            now = day(10, TUESDAY).atTime(LocalTime.of(23, 30)).toInstant(ZoneOffset.UTC);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(10, WEDNESDAY, SQUAT, "102.5", 5, 9);

            assertThat(progressOf(own(FOUR_WEEKS), SQUAT).isPrToday()).isTrue();
        }

        @Test
        void shouldNotBeOne_whenTheExerciseIsLoggedForTheFirstTimeToday() {
            now = wednesday(10);
            logged(10, WEDNESDAY, SQUAT, "102.5", 5, 9);

            assertThat(progressOf(own(FOUR_WEEKS), SQUAT).isPrToday()).isFalse();
        }

        @Test
        void shouldMarkThePrsOfTodaysSession_whenItHasAPrAnEqualSetAndAFirstLog() {
            now = wednesday(10);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(10, WEDNESDAY, SQUAT, "102.5", 5, 9);
            logged(9, MONDAY, ROW, "60.0", 10, 8);
            logged(10, WEDNESDAY, ROW, "60.0", 10, 9);
            logged(10, WEDNESDAY, PRESS, "40.0", 8, 8);

            List<ExerciseId> marked = own(FOUR_WEEKS).getExercises().stream()
                    .filter(ExerciseProgress::isPrToday)
                    .map(exercise -> exercise.getExercise().getId())
                    .toList();

            assertThat(marked)
                    .containsExactlyInAnyOrderElementsOf(rewards(day(10, WEDNESDAY)).prs().stream()
                            .map(Pr::exercise)
                            .toList())
                    .containsExactly(SQUAT.getId());
        }
    }

    @Nested
    class Exercises {

        @Test
        void shouldOrderByTheSessionsInThePeriod_thenByTheNewest() {
            now = wednesday(10);
            logged(7, MONDAY, SQUAT, "100.0", 5, 8);
            logged(8, MONDAY, SQUAT, "100.0", 5, 8);
            logged(9, MONDAY, SQUAT, "100.0", 5, 8);
            logged(8, WEDNESDAY, ROW, "60.0", 10, 8);
            logged(10, MONDAY, ROW, "60.0", 10, 8);
            logged(7, WEDNESDAY, PRESS, "40.0", 8, 8);
            logged(9, WEDNESDAY, PRESS, "40.0", 8, 8);
            // sessions before the period do not count
            for (int week = 2; week <= 5; week++) {
                logged(week, FRIDAY, PRESS, "40.0", 8, 8);
            }

            assertThat(own(FOUR_WEEKS).getExercises())
                    .extracting(ExerciseProgress::getExercise)
                    .containsExactly(SQUAT, ROW, PRESS);
        }

        @Test
        void shouldGiveTheThreeNewestSessionsOverAllLogs_butTheBestAndWeeksOfThePeriodOnly() {
            now = wednesday(10);
            logged(5, MONDAY, SQUAT, "110.0", 5, 9);
            logged(6, MONDAY, SQUAT, "100.0", 5, 8);
            logged(7, MONDAY, SQUAT, "102.5", 5, 7);

            ExerciseProgress squat = progressOf(own(FOUR_WEEKS), SQUAT);

            assertThat(squat.getRecent())
                    .extracting(ExerciseProgress.Recent::date)
                    .containsExactly(day(7, MONDAY), day(6, MONDAY), day(5, MONDAY));
            assertThat(squat.getBest()).isEqualTo(new TopSet(new BigDecimal("102.5"), 5, 7));
            assertThat(squat.getWeeks())
                    .containsExactly(new ExerciseProgress.Week(
                            7, new TopSet(new BigDecimal("102.5"), 5, 7), new BigDecimal("7.0")));
        }
    }

    @Nested
    class Readers {

        @Test
        void shouldRefuseAnotherTrainersClient_beforeReadingAPlanOrALog() {
            var service =
                    new ProgressViewService(clients, plans, accounts, trainingLog, Clock.fixed(now, ZoneOffset.UTC));

            assertThatThrownBy(() -> service.ofClient(OTHER_TRAINER, CLIENT, FOUR_WEEKS))
                    .isInstanceOf(ClientNotFoundException.class);
            assertThat(reads).isEmpty();
        }

        @Test
        void shouldGiveTheTrainerWhatTheClientReads() {
            finishWholeWeeks(1, 3);
            logged(3, MONDAY, SQUAT, "100.0", 5, 8);
            var service =
                    new ProgressViewService(clients, plans, accounts, trainingLog, Clock.fixed(now, ZoneOffset.UTC));

            assertThat(service.ofClient(TRAINER, CLIENT, FOUR_WEEKS))
                    .usingRecursiveComparison()
                    .isEqualTo(service.own(CLIENT, FOUR_WEEKS));
        }

        @Test
        void shouldBePlanNotFound_withoutAPublishedPlan() {
            plan = null;

            assertThatThrownBy(() -> own(FOUR_WEEKS)).isInstanceOf(PlanNotFoundException.class);
        }
    }
}
