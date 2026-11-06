package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.GeneratedPlanRejectedException;
import com.kinetiq.plan.domain.input.DraftInput;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.GeneratedPlan.GeneratedEntry;
import com.kinetiq.plan.domain.model.GeneratedPlan.GeneratedSession;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class GeneratedPlanChecksTest {

    private static final AccountId TRAINER = AccountId.random();

    private static final List<Exercise> LIBRARY = IntStream.rangeClosed(1, 11)
            .mapToObj(number -> exercise("Exercise " + number, Measure.REPS))
            .collect(Collectors.toCollection(ArrayList::new));
    private static final Exercise CARRY = exercise("Farmer's carry", Measure.SECONDS);

    static {
        LIBRARY.add(CARRY);
    }

    // immutable, as the prompt's map is: asking it for a null key throws
    private static final Map<String, ExerciseId> KEYS = Map.copyOf(IntStream.rangeClosed(1, 12)
            .boxed()
            .collect(Collectors.toMap(number -> "E" + number, number -> LIBRARY.get(number - 1).getId())));

    private static Exercise exercise(String name, Measure measure) {
        return new Exercise(
                ExerciseId.random(),
                null,
                new LocalizedText(name, ""),
                Category.LEGS,
                new LocalizedText("", ""),
                measure,
                3,
                10,
                90,
                new LocalizedText("", ""));
    }

    private static DraftInput input(int days, int age, String weightKg) {
        QuizSubmission answers = new QuizSubmission(
                QuizSubmissionId.random(),
                AccountId.random(),
                Goal.BUILD_MUSCLE,
                Experience.YEARS_1_TO_2,
                Level.INTERMEDIATE,
                age,
                178,
                new BigDecimal(weightKg),
                Set.of(Like.WEIGHTS),
                days,
                null,
                Instant.parse("2026-11-05T10:00:00Z"));
        return new DraftInput(answers, TRAINER, "Ola", "Marek", Language.EN, LIBRARY);
    }

    private static GeneratedEntry entry(String key) {
        return new GeneratedEntry(key, 3, 10, new BigDecimal("20.5"), 7, 90);
    }

    private static List<GeneratedEntry> entries(int count, int firstKey) {
        return IntStream.range(firstKey, firstKey + count)
                .mapToObj(number -> entry("E" + number))
                .toList();
    }

    private static GeneratedSession session(String name) {
        return new GeneratedSession(name, null, entries(4, 1));
    }

    /** A plan of these sessions on Monday, Tuesday and onwards, in the order given. */
    private static GeneratedPlan plan(GeneratedSession... sessions) {
        return new GeneratedPlan(IntStream.range(0, sessions.length)
                .mapToObj(index -> new GeneratedSession(
                        sessions[index].name(), DayOfWeek.of(index + 1).name(), sessions[index].entries()))
                .toList());
    }

    private static GeneratedSession session(String name, String day) {
        return new GeneratedSession(name, day, entries(4, 1));
    }

    /** Two valid sessions, the first holding {@code entry} after three valid ones. */
    private static GeneratedPlan withEntry(GeneratedEntry entry) {
        List<GeneratedEntry> first = new ArrayList<>(entries(3, 1));
        first.add(entry);
        return plan(new GeneratedSession("A", null, first), session("B"));
    }

    private static List<Session> check(GeneratedPlan plan) {
        return GeneratedPlanChecks.check(plan, KEYS, input(2, 30, "80.0"));
    }

    private static void assertRejected(GeneratedPlan plan, DraftInput input, String... codes) {
        assertThatThrownBy(() -> GeneratedPlanChecks.check(plan, KEYS, input))
                .isInstanceOf(GeneratedPlanRejectedException.class)
                .extracting(thrown -> ((GeneratedPlanRejectedException) thrown).codes())
                .asInstanceOf(InstanceOfAssertFactories.LIST)
                .containsExactlyInAnyOrder((Object[]) codes);
    }

    private static void assertRejected(GeneratedPlan plan, String... codes) {
        assertRejected(plan, input(2, 30, "80.0"), codes);
    }

    @Test
    void shouldPutEachSessionOnItsAnswerDay_whenValid() {
        GeneratedPlan plan = new GeneratedPlan(
                List.of(session("Push", "TUESDAY"), session("Pull", "THURSDAY"), session("Legs", "SATURDAY")));

        List<Session> sessions = GeneratedPlanChecks.check(plan, KEYS, input(3, 30, "80.0"));

        assertThat(sessions)
                .extracting(Session::name, Session::day)
                .containsExactly(
                        Tuple.tuple("Push", DayOfWeek.TUESDAY),
                        Tuple.tuple("Pull", DayOfWeek.THURSDAY),
                        Tuple.tuple("Legs", DayOfWeek.SATURDAY));
        assertThat(sessions.getFirst().entries().getFirst().exercise()).isEqualTo(KEYS.get("E1"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "Monday", "MON", "FUNDAY", " MONDAY"})
    void shouldRejectSessionDay_whenNotADayInCapitals(String day) {
        assertRejected(
                new GeneratedPlan(List.of(session("A", day), session("B", "THURSDAY"))), GeneratedPlanChecks.SESSION_DAY);
    }

    @Test
    void shouldRejectSessionDay_whenTwoSessionsShareADay() {
        assertRejected(
                new GeneratedPlan(List.of(session("A", "MONDAY"), session("B", "MONDAY"))),
                GeneratedPlanChecks.SESSION_DAY);
    }

    @Test
    void shouldAccept_whenALoadedExerciseIsMeasuredInSecondsAndOthersHaveNoLoad() {
        GeneratedEntry carry = new GeneratedEntry("E12", 3, 40, new BigDecimal("24.0"), 7, 60);
        GeneratedEntry bodyweight = new GeneratedEntry("E2", 3, 12, null, 7, 60);
        GeneratedPlan plan = plan(
                new GeneratedSession("A", null, List.of(carry, bodyweight, entry("E3"), entry("E4"))), session("B"));

        List<Session> sessions = check(plan);

        assertThat(sessions.getFirst().entries().getFirst().loadKg()).isEqualByComparingTo("24.0");
        assertThat(sessions.getFirst().entries().get(1).loadKg()).isNull();
    }

    @Test
    void shouldRejectEverySessionCount_exceptTheClientsDays() {
        assertRejected(plan(), GeneratedPlanChecks.SESSION_COUNT);
        assertRejected(plan(session("A")), GeneratedPlanChecks.SESSION_COUNT);
        assertRejected(
                plan(session("A"), session("B"), session("C")),
                GeneratedPlanChecks.SESSION_COUNT);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 11})
    void shouldRejectEntryCount_whenOutsideFourToTen(int count) {
        GeneratedPlan plan = plan(
                new GeneratedSession("A", null, count == 11 ? entries(11, 1) : entries(count, 1)), session("B"));

        assertRejected(plan, GeneratedPlanChecks.ENTRY_COUNT);
    }

    @ParameterizedTest
    @ValueSource(ints = {4, 10})
    void shouldAcceptEntryCount_whenAtTheLimits(int count) {
        GeneratedPlan plan = plan(new GeneratedSession("A", null, entries(count, 1)), session("B"));

        assertThat(check(plan)).hasSize(2);
    }

    @Test
    void shouldRejectUnknownExercise_whenKeyIsOutsideTheCatalogue() {
        assertRejected(withEntry(entry("E99")), GeneratedPlanChecks.UNKNOWN_EXERCISE);
    }

    @Test
    void shouldRejectUnknownExercise_whenKeyStandsForAnExerciseGoneFromTheLibrary() {
        Map<String, ExerciseId> keys = Map.of("E1", ExerciseId.random());
        GeneratedPlan plan = plan(new GeneratedSession("A", null, List.of(entry("E1"))));

        assertThatThrownBy(() -> GeneratedPlanChecks.check(plan, keys, input(2, 30, "80.0")))
                .isInstanceOf(GeneratedPlanRejectedException.class)
                .satisfies(thrown -> assertThat(((GeneratedPlanRejectedException) thrown).codes())
                        .contains(GeneratedPlanChecks.UNKNOWN_EXERCISE));
    }

    @Test
    void shouldRejectRepeatedExercise_whenTwiceInOneSession() {
        assertRejected(withEntry(entry("E1")), GeneratedPlanChecks.REPEATED_EXERCISE);
    }

    @Test
    void shouldAcceptTheSameExercise_whenInTwoSessions() {
        assertThat(check(plan(session("A"), session("B")))).hasSize(2);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "   ",
                "A name of forty-one characters, one extra",
                "Plan at https://example.com",
                "www.example.com",
                "example.com day",
                "Write to a@example.com"
            })
    void shouldRejectSessionName_whenBlankLongOrHoldingALink(String name) {
        assertRejected(plan(session(name), session("B")), GeneratedPlanChecks.SESSION_NAME);
    }

    @Test
    void shouldRejectSessionName_whenAbsent() {
        assertRejected(plan(session(null), session("B")), GeneratedPlanChecks.SESSION_NAME);
    }

    @Test
    void shouldRejectSessionName_whenNotUniqueAfterTrimmingAndIgnoringCase() {
        assertRejected(plan(session("Legs"), session(" LEGS ")), GeneratedPlanChecks.SESSION_NAME);
    }

    @Test
    void shouldAcceptSessionName_whenFortyCharactersAfterTrimming() {
        String forty = "  " + "x".repeat(40) + "  ";

        assertThat(check(plan(session(forty), session("B"))).getFirst().name())
                .hasSize(40);
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,true", "8,true", "9,false"})
    void shouldCheckSets(int sets, boolean accepted) {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", sets, 10, null, 7, 90));

        assertChecked(plan, accepted, GeneratedPlanChecks.SETS);
    }

    @ParameterizedTest
    @CsvSource({"E9,0,false", "E9,1,true", "E9,50,true", "E9,51,false", "E12,4,false", "E12,5,true", "E12,3600,true", "E12,3601,false"})
    void shouldCheckAmountByTheExercisesMeasure(String key, int amount, boolean accepted) {
        GeneratedPlan plan = withEntry(new GeneratedEntry(key, 3, amount, null, 7, 90));

        assertChecked(plan, accepted, GeneratedPlanChecks.AMOUNT);
    }

    @ParameterizedTest
    @CsvSource({"4,false", "5,true", "9,true", "10,false"})
    void shouldCheckRpe_whenAgeNeedsNoCaution(int rpe, boolean accepted) {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", 3, 10, null, rpe, 90));

        assertChecked(plan, accepted, GeneratedPlanChecks.RPE);
    }

    @ParameterizedTest
    @CsvSource({"16,false", "17,false", "18,true", "59,true", "60,false", "99,false"})
    void shouldAllowRpeNine_onlyBetween18And59(int age, boolean accepted) {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", 3, 10, null, 9, 90));
        DraftInput input = input(2, age, "80.0");

        if (accepted) {
            assertThat(GeneratedPlanChecks.check(plan, KEYS, input)).hasSize(2);
        } else {
            assertRejected(plan, input, GeneratedPlanChecks.RPE);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {17, 60})
    void shouldAcceptRpeEight_whenAgeNeedsCaution(int age) {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", 3, 10, null, 8, 90));

        assertThat(GeneratedPlanChecks.check(plan, KEYS, input(2, age, "80.0"))).hasSize(2);
    }

    @ParameterizedTest
    @CsvSource({"-1,false", "0,true", "300,true", "301,false"})
    void shouldCheckRest(int rest, boolean accepted) {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", 3, 10, null, 7, rest));

        assertChecked(plan, accepted, GeneratedPlanChecks.REST);
    }

    @ParameterizedTest
    @CsvSource({"0,false", "-5,false", "0.1,true", "12.25,false", "160.0,true", "160.1,false"})
    void shouldCheckLoad_againstTwiceTheBodyWeight(String load, boolean accepted) {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", 3, 10, new BigDecimal(load), 7, 90));

        assertChecked(plan, accepted, GeneratedPlanChecks.LOAD);
    }

    @Test
    void shouldNameMissingValue_whenATargetIsAbsent() {
        assertRejected(withEntry(new GeneratedEntry("E9", null, 10, null, 7, 90)), GeneratedPlanChecks.MISSING_VALUE);
        assertRejected(withEntry(new GeneratedEntry("E9", 3, 10, null, null, 90)), GeneratedPlanChecks.MISSING_VALUE);
        assertRejected(withEntry(new GeneratedEntry("E9", 3, 10, null, 7, null)), GeneratedPlanChecks.MISSING_VALUE);
        assertRejected(withEntry(new GeneratedEntry(null, 3, 10, null, 7, 90)), GeneratedPlanChecks.MISSING_VALUE);
        assertRejected(withEntry(new GeneratedEntry("E9", 3, null, null, 7, 90)), GeneratedPlanChecks.MISSING_VALUE);
    }

    // no-break spaces count as text for the name check, and only the plan rule trims them away
    @Test
    void shouldNamePlanRule_whenOnlyAPlanRuleIsBroken() {
        assertRejected(plan(session("\u00A0"), session("B")), GeneratedPlanChecks.PLAN_RULE);
    }

    @Test
    void shouldNameEveryBreach_whenSeveralApply() {
        GeneratedPlan plan = withEntry(new GeneratedEntry("E9", 9, 51, new BigDecimal("500.0"), 10, 301));

        assertRejected(
                plan,
                GeneratedPlanChecks.SETS,
                GeneratedPlanChecks.AMOUNT,
                GeneratedPlanChecks.LOAD,
                GeneratedPlanChecks.RPE,
                GeneratedPlanChecks.REST);
    }

    @Test
    void shouldReject_whenSessionHoldsOneFragment() {
        GeneratedPlan fragment = new GeneratedPlan(List.of(new GeneratedSession(",{", null, List.of())));

        assertRejected(
                fragment,
                GeneratedPlanChecks.SESSION_COUNT,
                GeneratedPlanChecks.SESSION_DAY,
                GeneratedPlanChecks.ENTRY_COUNT);
    }

    private static void assertChecked(GeneratedPlan plan, boolean accepted, String code) {
        if (accepted) {
            assertThat(check(plan)).hasSize(2);
        } else {
            assertRejected(plan, code);
        }
    }
}
