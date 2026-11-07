package com.kinetiq.plan.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import com.kinetiq.plan.domain.exception.PlanWeeksBeforeCurrentException;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class PlanTest {

    private static final AccountId CLIENT = AccountId.random();
    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final LocalDate MONDAY = LocalDate.parse("2026-10-26");

    private static ExerciseEntry entry() {
        return new ExerciseEntry(SQUAT, 3, 8, null, 8, 90);
    }

    private static Session session(DayOfWeek day) {
        return new Session("Lower body", day, List.of(entry()));
    }

    private static final List<Session> COMPLETE = List.of(session(DayOfWeek.MONDAY), session(DayOfWeek.THURSDAY));

    private static Plan draft(int weeks, List<Session> sessions) {
        return Plan.draft(CLIENT, Goal.BUILD_MUSCLE, weeks, sessions);
    }

    private static Plan published(int weeks, LocalDate firstWeek) {
        return new Plan(PlanId.random(), CLIENT, Goal.BUILD_MUSCLE, weeks, firstWeek, COMPLETE, 3);
    }

    @Nested
    class Marks {

        private final Plan built = Plan.built(CLIENT, Goal.BUILD_MUSCLE, 12, COMPLETE);
        private final Plan failed = Plan.failed(CLIENT, Goal.BUILD_MUSCLE, 12);

        @Test
        void shouldMarkABuiltDraft() {
            assertThat(built.getAutomaticDraft()).contains(AutomaticDraft.BUILT);
            assertThat(built.isPublished()).isFalse();
        }

        @Test
        void shouldMarkAFailedDraftAndLeaveItEmpty() {
            assertThat(failed.getAutomaticDraft()).contains(AutomaticDraft.FAILED);
            assertThat(failed.getSessions()).isEmpty();
            assertThat(failed.isPublished()).isFalse();
        }

        @Test
        void shouldHaveNoMark_whenBuiltByHand() {
            assertThat(draft(12, COMPLETE).getAutomaticDraft()).isEmpty();
        }

        @Test
        void shouldRefuseABuiltDraft_whenIncomplete() {
            assertRefused(
                    () -> Plan.built(CLIENT, Goal.BUILD_MUSCLE, 12, List.of(session(DayOfWeek.MONDAY))),
                    "plan.too-few-sessions");
        }

        @Test
        void shouldClearTheMark_whenEdited() {
            assertThat(built.edit(Goal.BUILD_MUSCLE, 12, COMPLETE, MONDAY).getAutomaticDraft()).isEmpty();
            assertThat(failed.edit(Goal.BUILD_MUSCLE, 12, List.of(), MONDAY).getAutomaticDraft()).isEmpty();
        }

        @Test
        void shouldClearTheMark_whenPublished() {
            assertThat(built.publish(MONDAY).getAutomaticDraft()).isEmpty();
        }

        @Test
        void shouldRefusePublishing_whenAFailedDraftIsStillEmpty() {
            assertRefused(() -> failed.publish(MONDAY), "plan.too-few-sessions");
        }
    }

    private static void assertRefused(Runnable action, String code) {
        assertThatThrownBy(action::run)
                .isInstanceOf(PlanRuleViolationException.class)
                .extracting("code")
                .isEqualTo(code);
    }

    @Nested
    class RebuiltWith {

        private final Plan saved = new Plan(PlanId.random(), CLIENT, Goal.LOSE_FAT, 8, null, List.of(), 4);
        private final List<Session> newSessions =
                List.of(session(DayOfWeek.TUESDAY), session(DayOfWeek.FRIDAY), session(DayOfWeek.SUNDAY));

        @Test
        void shouldReplaceTheContentAndMarkItBuilt_keepingIdClientAndVersion() {
            Plan rebuilt = saved.rebuiltWith(Goal.GET_STRONGER, 12, newSessions);

            assertThat(rebuilt.getId()).isEqualTo(saved.getId());
            assertThat(rebuilt.getClient()).isEqualTo(CLIENT);
            assertThat(rebuilt.getVersion()).isEqualTo(4);
            assertThat(rebuilt.getGoal()).isEqualTo(Goal.GET_STRONGER);
            assertThat(rebuilt.getWeeks()).isEqualTo(12);
            assertThat(rebuilt.getSessions()).isEqualTo(newSessions);
            assertThat(rebuilt.getAutomaticDraft()).contains(AutomaticDraft.BUILT);
            assertThat(rebuilt.isPublished()).isFalse();
        }

        @Test
        void shouldRefuseAnIncompleteResult() {
            assertRefused(
                    () -> saved.rebuiltWith(Goal.GET_STRONGER, 12, List.of(session(DayOfWeek.MONDAY))),
                    "plan.too-few-sessions");
        }

        @Test
        void shouldRefuseAPublishedPlan() {
            assertThatThrownBy(() -> published(12, MONDAY).rebuiltWith(Goal.GET_STRONGER, 12, newSessions))
                    .isInstanceOf(PlanAlreadyPublishedException.class);
        }
    }

    @Nested
    class Entries {

        private static ExerciseEntry load(String kg) {
            return new ExerciseEntry(SQUAT, 3, 8, kg == null ? null : new BigDecimal(kg), 8, 90);
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 20})
        void shouldAcceptSets_whenAtTheLimits(int sets) {
            assertThat(new ExerciseEntry(SQUAT, sets, 8, null, 8, 90).sets()).isEqualTo(sets);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 21})
        void shouldRefuseSets_whenOutOfRange(int sets) {
            assertRefused(() -> new ExerciseEntry(SQUAT, sets, 8, null, 8, 90), "plan.sets-out-of-range");
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 3600})
        void shouldAcceptAmount_whenAtTheLimits(int amount) {
            assertThat(new ExerciseEntry(SQUAT, 3, amount, null, 8, 90).amount()).isEqualTo(amount);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 3601})
        void shouldRefuseAmount_whenOutOfRange(int amount) {
            assertRefused(() -> new ExerciseEntry(SQUAT, 3, amount, null, 8, 90), "plan.amount-out-of-range");
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.1", "999.9", "100", "62.50"})
        void shouldAcceptLoad_whenAboveZeroWithAtMostOneDecimal(String kg) {
            assertThat(load(kg).loadKg()).isEqualByComparingTo(kg);
        }

        @Test
        void shouldKeepAnEmptyLoadEmpty() {
            assertThat(load(null).loadKg()).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"0", "0.0", "-1", "1000", "999.95", "62.55"})
        void shouldRefuseLoad_whenOutOfRangeOrTooPrecise(String kg) {
            assertRefused(() -> load(kg), "plan.load-out-of-range");
        }

        @ParameterizedTest
        @ValueSource(ints = {5, 10})
        void shouldAcceptRpe_whenAtTheLimits(int rpe) {
            assertThat(new ExerciseEntry(SQUAT, 3, 8, null, rpe, 90).rpe()).isEqualTo(rpe);
        }

        @ParameterizedTest
        @ValueSource(ints = {4, 11})
        void shouldRefuseRpe_whenOutOfRange(int rpe) {
            assertRefused(() -> new ExerciseEntry(SQUAT, 3, 8, null, rpe, 90), "plan.rpe-out-of-range");
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 600})
        void shouldAcceptRest_whenAtTheLimits(int rest) {
            assertThat(new ExerciseEntry(SQUAT, 3, 8, null, 8, rest).restSeconds()).isEqualTo(rest);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 601})
        void shouldRefuseRest_whenOutOfRange(int rest) {
            assertRefused(() -> new ExerciseEntry(SQUAT, 3, 8, null, 8, rest), "plan.rest-out-of-range");
        }
    }

    @Nested
    class Sessions {

        @Test
        void shouldTrimTheName_includingNoBreakSpaces() {
            assertThat(new Session("  Lower body  ", DayOfWeek.MONDAY, List.of()).name()).isEqualTo("Lower body");
        }

        @Test
        void shouldAcceptTheName_whenSixtyCharactersAfterTrimming() {
            String name = "x".repeat(60);

            assertThat(new Session("  " + name + "  ", DayOfWeek.MONDAY, List.of()).name()).isEqualTo(name);
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   ", " "})
        void shouldRefuseTheName_whenEmptyAfterTrimming(String name) {
            assertRefused(() -> new Session(name, DayOfWeek.MONDAY, List.of()), "plan.session-name-invalid");
        }

        @Test
        void shouldRefuseTheName_whenLongerThanSixtyCharacters() {
            assertRefused(
                    () -> new Session("x".repeat(61), DayOfWeek.MONDAY, List.of()), "plan.session-name-invalid");
        }

        @Test
        void shouldAcceptThirtyExercises_includingTheSameOneTwice() {
            List<ExerciseEntry> entries = Collections.nCopies(30, entry());

            assertThat(new Session("Long", DayOfWeek.MONDAY, entries).entries()).hasSize(30);
        }

        @Test
        void shouldRefuseThirtyOneExercises() {
            assertRefused(
                    () -> new Session("Long", DayOfWeek.MONDAY, Collections.nCopies(31, entry())),
                    "plan.too-many-exercises");
        }
    }

    @Nested
    class EveryPlan {

        @ParameterizedTest
        @ValueSource(ints = {4, 16})
        void shouldAcceptTheLength_whenAtTheLimits(int weeks) {
            assertThat(draft(weeks, List.of()).getWeeks()).isEqualTo(weeks);
        }

        @ParameterizedTest
        @ValueSource(ints = {3, 17})
        void shouldRefuseTheLength_whenOutOfRange(int weeks) {
            assertRefused(() -> draft(weeks, List.of()), "plan.weeks-out-of-range");
        }

        @Test
        void shouldAcceptSixSessions_onAnyDayOfTheWeek() {
            List<Session> sessions = List.of(
                    session(DayOfWeek.SUNDAY),
                    session(DayOfWeek.SATURDAY),
                    session(DayOfWeek.FRIDAY),
                    session(DayOfWeek.WEDNESDAY),
                    session(DayOfWeek.TUESDAY),
                    session(DayOfWeek.MONDAY));

            assertThat(draft(12, sessions).getSessions()).hasSize(6);
        }

        @Test
        void shouldRefuseSevenSessions() {
            List<Session> sessions = List.of(DayOfWeek.values()).stream().map(PlanTest::session).toList();

            assertRefused(() -> draft(12, sessions), "plan.too-many-sessions");
        }

        @Test
        void shouldRefuseTwoSessionsOnOneDay() {
            assertRefused(
                    () -> draft(12, List.of(session(DayOfWeek.MONDAY), session(DayOfWeek.MONDAY))),
                    "plan.duplicate-day");
        }

        @Test
        void shouldOrderSessionsByDay_mondayFirst() {
            Plan plan = draft(12, List.of(session(DayOfWeek.SUNDAY), session(DayOfWeek.MONDAY), session(DayOfWeek.WEDNESDAY)));

            assertThat(plan.getSessions())
                    .extracting(Session::day)
                    .containsExactly(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY, DayOfWeek.SUNDAY);
        }

        @Test
        void shouldBeADraft_whenThereIsNoFirstWeek() {
            assertThat(draft(12, List.of()).isPublished()).isFalse();
            assertThat(published(12, MONDAY).isPublished()).isTrue();
        }
    }

    @Nested
    class RequireComplete {

        @Test
        void shouldPass_whenTwoSessionsEachHaveAnExercise() {
            assertThatCode(() -> draft(12, COMPLETE).requireComplete()).doesNotThrowAnyException();
        }

        @Test
        void shouldRefuse_whenFewerThanTwoSessions() {
            Plan plan = draft(12, List.of(session(DayOfWeek.MONDAY)));

            assertRefused(plan::requireComplete, "plan.too-few-sessions");
        }

        @Test
        void shouldRefuse_whenASessionHasNoExercises() {
            Plan plan = draft(12, List.of(session(DayOfWeek.MONDAY), new Session("Empty", DayOfWeek.FRIDAY, List.of())));

            assertRefused(plan::requireComplete, "plan.session-without-exercises");
        }
    }

    @Nested
    class Edit {

        // in week 6 of the plan
        private static final LocalDate IN_WEEK_6 = MONDAY.plusWeeks(5).plusDays(2);

        @Test
        void shouldAllowAnIncompleteDraft() {
            Plan edited = draft(12, COMPLETE).edit(Goal.LOSE_FAT, 8, List.of(), MONDAY);

            assertThat(edited.getGoal()).isEqualTo(Goal.LOSE_FAT);
            assertThat(edited.getWeeks()).isEqualTo(8);
            assertThat(edited.getSessions()).isEmpty();
        }

        @Test
        void shouldKeepIdFirstWeekAndVersion_whenEditingAPublishedPlan() {
            Plan plan = published(16, MONDAY);

            Plan edited = plan.edit(Goal.LOSE_FAT, 16, COMPLETE, IN_WEEK_6);

            assertThat(edited.getId()).isEqualTo(plan.getId());
            assertThat(edited.getFirstWeek()).contains(MONDAY);
            assertThat(edited.getVersion()).isEqualTo(plan.getVersion());
            assertThat(edited.getGoal()).isEqualTo(Goal.LOSE_FAT);
        }

        @Test
        void shouldRefuseAnIncompletePlan_whenPublished() {
            Plan plan = published(16, MONDAY);

            assertRefused(
                    () -> plan.edit(Goal.LOSE_FAT, 16, List.of(session(DayOfWeek.MONDAY)), IN_WEEK_6),
                    "plan.too-few-sessions");
        }

        @Test
        void shouldAllowShortening_downToTheCurrentWeek() {
            assertThat(published(16, MONDAY).edit(Goal.LOSE_FAT, 6, COMPLETE, IN_WEEK_6).getWeeks()).isEqualTo(6);
        }

        @Test
        void shouldRefuseShortening_belowTheCurrentWeek_namingIt() {
            Plan plan = published(16, MONDAY);

            assertThatThrownBy(() -> plan.edit(Goal.LOSE_FAT, 5, COMPLETE, IN_WEEK_6))
                    .isInstanceOf(PlanWeeksBeforeCurrentException.class)
                    .satisfies(exception -> assertThat(((PlanWeeksBeforeCurrentException) exception).properties())
                            .containsEntry("currentWeek", 6));
        }

        @Test
        void shouldSaveAFinishedPlan_whenItsLengthStaysTheSame() {
            LocalDate longAfter = MONDAY.plusWeeks(20);

            assertThat(published(8, MONDAY).edit(Goal.LOSE_FAT, 8, COMPLETE, longAfter).getWeeks()).isEqualTo(8);
        }

        @Test
        void shouldLengthenAFinishedPlan_evenWhenStillBelowTheCurrentWeek() {
            LocalDate longAfter = MONDAY.plusWeeks(20);

            assertThat(published(8, MONDAY).edit(Goal.LOSE_FAT, 10, COMPLETE, longAfter).getWeeks()).isEqualTo(10);
        }

        @Test
        void shouldRefuseShorteningAFinishedPlan() {
            LocalDate longAfter = MONDAY.plusWeeks(20);

            assertThatThrownBy(() -> published(8, MONDAY).edit(Goal.LOSE_FAT, 7, COMPLETE, longAfter))
                    .isInstanceOf(PlanWeeksBeforeCurrentException.class);
        }
    }

    @Nested
    class Publish {

        @ParameterizedTest
        @ValueSource(strings = {"2026-11-02", "2026-11-04", "2026-11-08"})
        void shouldStartWeekOneOnTheMondayOfThatWeek(LocalDate today) {
            Plan published = draft(12, COMPLETE).publish(today);

            assertThat(published.isPublished()).isTrue();
            assertThat(published.getFirstWeek()).contains(LocalDate.parse("2026-11-02"));
            assertThat(published.currentWeek(today)).contains(1);
        }

        @Test
        void shouldRefuse_whenTheDraftIsNotComplete() {
            Plan draft = draft(12, List.of(session(DayOfWeek.MONDAY)));

            assertRefused(() -> draft.publish(MONDAY), "plan.too-few-sessions");
        }

        @Test
        void shouldRefuse_whenASessionHasNoExercises() {
            Plan draft = draft(12, List.of(session(DayOfWeek.MONDAY), new Session("Empty", DayOfWeek.FRIDAY, List.of())));

            assertRefused(() -> draft.publish(MONDAY), "plan.session-without-exercises");
        }

        @Test
        void shouldRefuse_whenPublishedAlready() {
            Plan published = published(12, MONDAY);

            assertThatThrownBy(() -> published.publish(MONDAY)).isInstanceOf(PlanAlreadyPublishedException.class);
        }
    }

    @Nested
    class CurrentWeek {

        @Test
        void shouldBeEmpty_whenDraft() {
            assertThat(draft(12, List.of()).currentWeek(MONDAY)).isEmpty();
        }

        @ParameterizedTest
        @CsvSource({
            "2026-10-26, 1", // the first Monday
            "2026-11-01, 1", // its Sunday
            "2026-11-02, 2", // the next Monday
            "2026-12-27, 9",
            "2027-01-04, 11" // across a year's end
        })
        void shouldCountMondayToSundayWeeks(LocalDate today, int week) {
            assertThat(published(16, MONDAY).currentWeek(today)).contains(week);
        }

        @ParameterizedTest
        @ValueSource(strings = {"2026-10-25", "2026-09-01"})
        void shouldBeOne_whenBeforeTheFirstWeek(LocalDate today) {
            assertThat(published(16, MONDAY).currentWeek(today)).contains(1);
        }

        @Test
        void shouldExceedTheLength_whenFinished() {
            assertThat(published(4, MONDAY).currentWeek(MONDAY.plusWeeks(4))).contains(5);
        }
    }
}
