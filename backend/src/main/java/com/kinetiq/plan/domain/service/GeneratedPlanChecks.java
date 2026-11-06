package com.kinetiq.plan.domain.service;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.GeneratedPlanRejectedException;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import com.kinetiq.plan.domain.input.DraftInput;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Decides whether an answer of the drafting service may become a draft. The limits are narrower than the plan rules,
 * which allow values no first plan should have; a trainer may still go beyond them by hand.
 */
final class GeneratedPlanChecks {

    static final String SESSION_COUNT = "draft.session-count";
    static final String SESSION_DAY = "draft.session-day";
    static final String ENTRY_COUNT = "draft.entry-count";
    static final String UNKNOWN_EXERCISE = "draft.unknown-exercise";
    static final String REPEATED_EXERCISE = "draft.repeated-exercise";
    static final String SESSION_NAME = "draft.session-name";
    static final String SETS = "draft.sets";
    static final String AMOUNT = "draft.amount";
    static final String RPE = "draft.rpe";
    static final String REST = "draft.rest";
    static final String LOAD = "draft.load";
    static final String MISSING_VALUE = "draft.missing-value";
    static final String PLAN_RULE = "draft.plan-rule";

    private static final int MIN_ENTRIES = 4;
    private static final int MAX_ENTRIES = 10;
    private static final int MAX_NAME_LENGTH = 40;
    private static final int MIN_SETS = 1;
    private static final int MAX_SETS = 8;
    private static final int MIN_REPETITIONS = 1;
    private static final int MAX_REPETITIONS = 50;
    private static final int MIN_SECONDS = 5;
    private static final int MAX_SECONDS = 3600;
    private static final int MIN_RPE = 5;
    private static final int MAX_RPE = 9;
    private static final int MAX_RPE_CAUTIOUS = 8;
    private static final int FIRST_ADULT_AGE = 18;
    private static final int FIRST_OLDER_AGE = 60;
    private static final int MIN_REST = 0;
    private static final int MAX_REST = 300;
    private static final int LOAD_DECIMALS = 1;
    private static final BigDecimal MAX_LOAD_PER_BODY_WEIGHT = BigDecimal.TWO;

    private static final Pattern LINK_OR_EMAIL =
            Pattern.compile("://|www\\.|@|\\b[\\p{L}\\p{N}-]+\\.[a-zA-Z]{2,}\\b", Pattern.CASE_INSENSITIVE);

    private GeneratedPlanChecks() {}

    /**
     * Returns the sessions of an acceptable answer, each on the day the answer gave it. Otherwise throws
     * {@link GeneratedPlanRejectedException} with every code that applies.
     */
    static List<Session> check(GeneratedPlan plan, Map<String, ExerciseId> keys, DraftInput input) {
        Map<ExerciseId, Exercise> library =
                input.library().stream().collect(Collectors.toMap(Exercise::getId, Function.identity()));
        QuizSubmission answers = input.answers();
        Set<String> codes = new LinkedHashSet<>();
        if (plan.sessions().size() != answers.getDaysPerWeek()) {
            codes.add(SESSION_COUNT);
        }
        checkDays(plan.sessions(), codes);
        checkNames(plan.sessions(), codes);
        for (GeneratedPlan.GeneratedSession session : plan.sessions()) {
            checkEntries(session.entries(), keys, library, answers, codes);
        }
        if (!codes.isEmpty()) {
            throw new GeneratedPlanRejectedException(codes);
        }
        return toSessions(plan.sessions(), keys);
    }

    private static void checkDays(List<GeneratedPlan.GeneratedSession> sessions, Set<String> codes) {
        Set<DayOfWeek> seen = new HashSet<>();
        for (GeneratedPlan.GeneratedSession session : sessions) {
            if (day(session).filter(seen::add).isEmpty()) {
                codes.add(SESSION_DAY);
            }
        }
    }

    private static Optional<DayOfWeek> day(GeneratedPlan.GeneratedSession session) {
        return Arrays.stream(DayOfWeek.values())
                .filter(day -> day.name().equals(session.day()))
                .findFirst();
    }

    private static void checkNames(List<GeneratedPlan.GeneratedSession> sessions, Set<String> codes) {
        Set<String> seen = new HashSet<>();
        for (GeneratedPlan.GeneratedSession session : sessions) {
            String name = session.name();
            if (name == null
                    || name.isBlank()
                    || name.strip().length() > MAX_NAME_LENGTH
                    || LINK_OR_EMAIL.matcher(name).find()
                    || !seen.add(name.strip().toLowerCase(Locale.ROOT))) {
                codes.add(SESSION_NAME);
            }
        }
    }

    private static void checkEntries(
            List<GeneratedPlan.GeneratedEntry> entries,
            Map<String, ExerciseId> keys,
            Map<ExerciseId, Exercise> library,
            QuizSubmission answers,
            Set<String> codes) {
        if (entries.size() < MIN_ENTRIES || entries.size() > MAX_ENTRIES) {
            codes.add(ENTRY_COUNT);
        }
        Set<String> seen = new HashSet<>();
        for (GeneratedPlan.GeneratedEntry entry : entries) {
            if (entry.exerciseKey() != null && !seen.add(entry.exerciseKey())) {
                codes.add(REPEATED_EXERCISE);
            }
            checkEntry(entry, keys, library, answers, codes);
        }
    }

    private static void checkEntry(
            GeneratedPlan.GeneratedEntry entry,
            Map<String, ExerciseId> keys,
            Map<ExerciseId, Exercise> library,
            QuizSubmission answers,
            Set<String> codes) {
        if (entry.exerciseKey() == null
                || entry.sets() == null
                || entry.amount() == null
                || entry.rpe() == null
                || entry.restSeconds() == null) {
            codes.add(MISSING_VALUE);
        }
        Exercise exercise = entry.exerciseKey() == null ? null : library.get(keys.get(entry.exerciseKey()));
        if (entry.exerciseKey() != null && exercise == null) {
            codes.add(UNKNOWN_EXERCISE);
        }
        if (outside(entry.sets(), MIN_SETS, MAX_SETS)) {
            codes.add(SETS);
        }
        if (exercise != null && amountOutside(entry.amount(), exercise.getMeasure())) {
            codes.add(AMOUNT);
        }
        if (outside(entry.rpe(), MIN_RPE, maxRpe(answers.getAge()))) {
            codes.add(RPE);
        }
        if (outside(entry.restSeconds(), MIN_REST, MAX_REST)) {
            codes.add(REST);
        }
        if (entry.loadKg() != null && invalidLoad(entry.loadKg(), answers.getWeightKg())) {
            codes.add(LOAD);
        }
    }

    private static boolean amountOutside(Integer amount, Measure measure) {
        return measure == Measure.SECONDS
                ? outside(amount, MIN_SECONDS, MAX_SECONDS)
                : outside(amount, MIN_REPETITIONS, MAX_REPETITIONS);
    }

    private static int maxRpe(int age) {
        return age < FIRST_ADULT_AGE || age >= FIRST_OLDER_AGE ? MAX_RPE_CAUTIOUS : MAX_RPE;
    }

    private static boolean invalidLoad(BigDecimal load, BigDecimal bodyWeight) {
        return load.signum() <= 0
                || load.stripTrailingZeros().scale() > LOAD_DECIMALS
                || load.compareTo(bodyWeight.multiply(MAX_LOAD_PER_BODY_WEIGHT)) > 0;
    }

    // a missing value has its own code
    private static boolean outside(Integer value, int min, int max) {
        return value != null && (value < min || value > max);
    }

    private static List<Session> toSessions(
            List<GeneratedPlan.GeneratedSession> sessions, Map<String, ExerciseId> keys) {
        try {
            return sessions.stream()
                    .map(session -> new Session(
                            session.name(),
                            day(session).orElseThrow(),
                            session.entries().stream().map(entry -> toEntry(entry, keys)).toList()))
                    .toList();
        } catch (PlanRuleViolationException exception) {
            throw new GeneratedPlanRejectedException(List.of(PLAN_RULE));
        }
    }

    private static ExerciseEntry toEntry(GeneratedPlan.GeneratedEntry entry, Map<String, ExerciseId> keys) {
        return new ExerciseEntry(
                keys.get(entry.exerciseKey()),
                entry.sets(),
                entry.amount(),
                entry.loadKg(),
                entry.rpe(),
                entry.restSeconds());
    }
}
