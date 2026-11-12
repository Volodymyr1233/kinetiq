package com.kinetiq.overview.domain.model;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.overview.domain.model.ClientTraining.DayLog;
import com.kinetiq.overview.domain.model.ClientTraining.WeekSessions;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ClientTrainingTest {

    private static final LocalDate FIRST_MONDAY = LocalDate.parse("2026-10-05");
    // Thursday of plan week 6
    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");

    // Monday, Wednesday and Friday, published on the Wednesday of week 1; 8 weeks end on Sunday 29 November
    private static final PlanDates THREE_A_WEEK =
            new PlanDates(FIRST_MONDAY, FIRST_MONDAY.plusDays(2), 8, Set.of(MONDAY, WEDNESDAY, FRIDAY));

    private static final PlanDates DAILY =
            new PlanDates(FIRST_MONDAY, FIRST_MONDAY, 8, EnumSet.allOf(DayOfWeek.class));

    private final Map<LocalDate, DayLog> days = new HashMap<>();

    private void log(String date, boolean finished, int... rpes) {
        int sum = 0;
        for (int rpe : rpes) {
            sum += rpe;
        }
        days.put(LocalDate.parse(date), new DayLog(finished, new RpeTotal(sum, rpes.length)));
    }

    private void finished(String... dates) {
        for (String date : dates) {
            log(date, true, 7);
        }
    }

    private ClientTraining training(PlanDates plan) {
        return new ClientTraining(plan, days, null, Map.of());
    }

    @Nested
    class CompletionOf {

        // the 12 sessions from Friday 16 October, 27 days back, to yesterday
        private static final String[] DUE = {
            "2026-10-16", "2026-10-19", "2026-10-21", "2026-10-23", "2026-10-26", "2026-10-28",
            "2026-10-30", "2026-11-02", "2026-11-04", "2026-11-06", "2026-11-09", "2026-11-11"
        };

        @Test
        void shouldCountEverySessionSincePassedInThe28Days_whenAllWereDone() {
            finished(DUE);

            assertThat(training(THREE_A_WEEK).completion(TODAY)).contains(new Completion(12, 12));
        }

        @Test
        void shouldCountTheMissedOnesAsDue_whenSomeWereNeverStarted() {
            finished("2026-10-16", "2026-10-19", "2026-11-11");

            assertThat(training(THREE_A_WEEK).completion(TODAY)).contains(new Completion(3, 12));
        }

        @Test
        void shouldIncludeThe28thDayBack_andNotThe29th() {
            finished("2026-10-15", "2026-10-16");

            Completion completion = training(DAILY).completion(TODAY).orElseThrow();

            assertThat(completion.due()).isEqualTo(27);
            assertThat(completion.done()).isEqualTo(1);
        }

        @Test
        void shouldNotCountTodaysSessionAsDue_untilItIsFinished() {
            LocalDate wednesday = LocalDate.parse("2026-11-11");
            log("2026-11-11", false, 8);

            assertThat(training(THREE_A_WEEK).completion(wednesday).orElseThrow().due()).isEqualTo(11);

            finished("2026-11-11");

            assertThat(training(THREE_A_WEEK).completion(wednesday)).contains(new Completion(1, 12));
        }

        @Test
        void shouldCountAStartedSessionAsMissed_whenItsDayHasPassedUnfinished() {
            log("2026-11-11", false, 8, 8);

            assertThat(training(THREE_A_WEEK).completion(TODAY)).contains(new Completion(0, 12));
        }

        @Test
        void shouldNotCountAFinishedSession_whenThePlanHasNoSessionThatDay() {
            finished("2026-11-10");

            assertThat(training(THREE_A_WEEK).completion(TODAY)).contains(new Completion(0, 12));
        }

        @Test
        void shouldStartOnThePublishDay_whenThePlanIsYoungerThan28Days() {
            // Friday of week 2: Wednesday and Friday of week 1, Monday and Wednesday of week 2 are due
            LocalDate friday = LocalDate.parse("2026-10-16");
            finished("2026-10-05", "2026-10-07");

            assertThat(training(THREE_A_WEEK).completion(friday)).contains(new Completion(1, 4));
        }

        @Test
        void shouldBeEmpty_whenNoSessionIsDueYet() {
            // the publish day, a Wednesday with its session still to do
            assertThat(training(THREE_A_WEEK).completion(LocalDate.parse("2026-10-07")))
                    .isEmpty();
        }

        @Test
        void shouldRunOnThePlansLastDay_andBeEmptyTheDayAfter() {
            ClientTraining training = training(THREE_A_WEEK);

            assertThat(training.completion(LocalDate.parse("2026-11-29"))).isPresent();
            assertThat(training.running(LocalDate.parse("2026-11-30"))).isFalse();
            assertThat(training.completion(LocalDate.parse("2026-11-30"))).isEmpty();
        }

        @Test
        void shouldBeEmpty_withoutAPublishedPlan() {
            finished(DUE);

            assertThat(training(null).completion(TODAY)).isEmpty();
        }
    }

    @Nested
    class Rpe {

        @Test
        void shouldAverageEverySetFromTheFirstToTheLastDay_finishedOrNot() {
            log("2026-10-29", true, 10);
            log("2026-10-30", true, 8, 9);
            log("2026-11-11", false, 7);
            log("2026-11-12", true, 9);
            log("2026-11-13", true, 5);

            RpeTotal total = training(THREE_A_WEEK).rpe(TODAY.minusDays(13), TODAY);

            assertThat(total).isEqualTo(new RpeTotal(33, 4));
            assertThat(total.mean()).contains(new BigDecimal("8.3"));
        }

        @Test
        void shouldRoundHalfUp_toOneDecimal() {
            // 8.05
            assertThat(new RpeTotal(161, 20).mean()).contains(new BigDecimal("8.1"));
        }

        @Test
        void shouldHaveNoMean_withoutSets() {
            assertThat(training(THREE_A_WEEK).rpe(TODAY.minusDays(13), TODAY).mean())
                    .isEmpty();
        }
    }

    @Nested
    class Week {

        @Test
        void shouldCountTheSessionsStillToCome_andDoneOnlyWhenFinished() {
            LocalDate monday = LocalDate.parse("2026-11-09");
            finished("2026-11-09");
            log("2026-11-11", false, 7);

            assertThat(training(THREE_A_WEEK).week(monday)).isEqualTo(new WeekSessions(3, 1));
        }

        @Test
        void shouldSkipTheDaysBeforeThePublishDay_inTheFirstWeek() {
            finished("2026-10-05");

            assertThat(training(THREE_A_WEEK).week(FIRST_MONDAY)).isEqualTo(new WeekSessions(2, 0));
        }

        @Test
        void shouldPlanNothing_withoutAPublishedPlan() {
            assertThat(training(null).week(FIRST_MONDAY)).isEqualTo(new WeekSessions(0, 0));
        }
    }

    @Test
    void shouldNumberPlanWeeksFromTheFirstMonday() {
        assertThat(THREE_A_WEEK.week(LocalDate.parse("2026-10-11"))).isEqualTo(1);
        assertThat(THREE_A_WEEK.week(LocalDate.parse("2026-10-12"))).isEqualTo(2);
        assertThat(THREE_A_WEEK.week(TODAY)).isEqualTo(6);
        assertThat(THREE_A_WEEK.lastDay()).isEqualTo(LocalDate.parse("2026-11-29"));
    }
}
