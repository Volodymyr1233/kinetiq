package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The title and reason of a plan change in English or Polish, built from its stored numbers. The only names in it are
 * exercise names; it never uses a pronoun, so it fits any reader.
 */
public final class PlanChangeWording {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final MathContext PRECISION = MathContext.DECIMAL64;

    private PlanChangeWording() {}

    /** The text of {@code planChange} in {@code language}; {@code names} holds exercise names in that language. */
    public static PlanChangeText of(Language language, PlanChange planChange, Map<ExerciseId, String> names) {
        boolean polish = language == Language.PL;
        int weeks = weeks(planChange.getFigures());
        return switch (planChange.getRule()) {
            case HIGH_RPE -> highRpe(polish, planChange, names, weeks);
            case STALLED_WEIGHT -> stalledWeight(polish, planChange, names, weeks);
            case LOW_COMPLETION -> lowCompletion(polish, planChange, weeks);
        };
    }

    private static PlanChangeText highRpe(
            boolean polish, PlanChange planChange, Map<ExerciseId, String> names, int weeks) {
        List<TargetChange> changes = planChange.getContent().changes();
        // per entry the volume ratio is the product of the changed factors, as an unchanged one cancels out
        // ponytail: plain mean over the changed entries, since unchanged sets are not stored; weight by volume if stored
        Map<EntryRef, List<TargetChange>> byEntry =
                changes.stream().collect(Collectors.groupingBy(TargetChange::entry));
        BigDecimal ratio = byEntry.values().stream()
                .map(entry -> entry.stream()
                        .filter(change -> change.target() == Target.SETS || change.target() == Target.AMOUNT)
                        .map(change -> change.newValue().divide(change.oldValue(), PRECISION))
                        .reduce(BigDecimal.ONE, BigDecimal::multiply))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, byEntry.size())), PRECISION);
        String percent = wholePercent(BigDecimal.ONE.subtract(ratio));
        String means = joined(
                polish,
                figures(planChange, FigureKind.MEAN_RPE).stream().map(figure -> decimal(polish, figure.value())).toList());
        String title = exerciseTitle(
                polish,
                exercises(changes),
                names,
                polish ? "obniż objętość o %s%%" : "cut volume by %s%%",
                polish ? "Obniż objętość w %d ćwiczeniach o %s%%" : "Cut volume on %d exercises by %s%%",
                percent);
        String reason = polish
                ? "Średnie RPE wynosiło %s w ostatnich %d tygodniach, powyżej 8.".formatted(means, weeks)
                : "Average RPE was %s in the last %d weeks, above 8.".formatted(means, weeks);
        return new PlanChangeText(title, reason);
    }

    private static PlanChangeText stalledWeight(
            boolean polish, PlanChange planChange, Map<ExerciseId, String> names, int weeks) {
        List<TargetChange> loads = planChange.getContent().changes().stream()
                .filter(change -> change.target() == Target.LOAD_KG)
                .toList();
        BigDecimal meanRise = loads.stream()
                .map(change -> change.newValue().divide(change.oldValue(), PRECISION).subtract(BigDecimal.ONE))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(Math.max(1, loads.size())), PRECISION);
        String title = exerciseTitle(
                polish,
                exercises(loads),
                names,
                polish ? "podnieś obciążenie o %s%%" : "raise load by %s%%",
                polish ? "Podnieś obciążenie w %d ćwiczeniach o %s%%" : "Raise load on %d exercises by %s%%",
                wholePercent(meanRise));
        Set<ExerciseId> stalled = planChange.getConcernedExercises();
        String reason;
        if (stalled.size() == 1 && names.containsKey(stalled.iterator().next())) {
            ExerciseId exercise = stalled.iterator().next();
            String rise = decimal(polish, rise(planChange, exercise));
            reason = polish
                    ? "%s: wzrost o %s%% w ciągu %d tygodni, mniej niż 5%%.".formatted(names.get(exercise), rise, weeks)
                    : "%s rose %s%% in %d weeks, less than 5%%.".formatted(names.get(exercise), rise, weeks);
        } else {
            int count = stalled.size();
            reason = polish
                    ? "%d %s: wzrost obciążenia poniżej 5%% w ciągu %d tygodni."
                            .formatted(count, plural(count, "ćwiczenie", "ćwiczenia", "ćwiczeń"), weeks)
                    : "%d exercises rose less than 5%% in %d weeks.".formatted(count, weeks);
        }
        return new PlanChangeText(title, reason);
    }

    private static PlanChangeText lowCompletion(boolean polish, PlanChange planChange, int weeks) {
        List<Figure> planned = figures(planChange, FigureKind.SESSIONS_PLANNED);
        BigDecimal plannedSum = sum(planned);
        BigDecimal finishedSum = sum(figures(planChange, FigureKind.SESSIONS_FINISHED));
        // a full week plans every session of the plan, and one of them goes
        int left = planned.stream().map(Figure::value).max(Comparator.naturalOrder()).orElse(BigDecimal.ONE).intValue()
                - 1;
        String share = plannedSum.signum() == 0 ? "0" : wholePercent(finishedSum.divide(plannedSum, PRECISION));
        String title = polish
                ? "Ograniczenie do %d sesji w tygodniu".formatted(left)
                : "Cut to %d sessions a week".formatted(left);
        String reason = polish
                ? "Ukończono %s%% zaplanowanych sesji w ostatnich %d tygodniach, poniżej 70%%.".formatted(share, weeks)
                : "%s%% of planned sessions were finished in the last %d weeks, below 70%%.".formatted(share, weeks);
        return new PlanChangeText(title, reason);
    }

    // one exercise is named, before the change so its own capitals stay; several are counted
    private static String exerciseTitle(
            boolean polish,
            Set<ExerciseId> exercises,
            Map<ExerciseId, String> names,
            String one,
            String several,
            String percent) {
        if (exercises.size() > 1) {
            return several.formatted(exercises.size(), percent);
        }
        String change = one.formatted(percent);
        String name = exercises.isEmpty() ? null : names.get(exercises.iterator().next());
        return name == null ? Character.toUpperCase(change.charAt(0)) + change.substring(1) : name + ": " + change;
    }

    private static Set<ExerciseId> exercises(List<TargetChange> changes) {
        return changes.stream().map(TargetChange::exercise).collect(Collectors.toSet());
    }

    private static List<Figure> figures(PlanChange planChange, FigureKind kind) {
        return planChange.getFigures().stream()
                .filter(figure -> figure.kind() == kind)
                .sorted(Comparator.comparing(Figure::weekStart))
                .toList();
    }

    // the rise of the best load from the first week to the last, in percent
    private static BigDecimal rise(PlanChange planChange, ExerciseId exercise) {
        List<Figure> best = figures(planChange, FigureKind.BEST_LOAD_KG).stream()
                .filter(figure -> figure.exercise().filter(exercise::equals).isPresent())
                .toList();
        if (best.isEmpty() || best.getFirst().value().signum() == 0) {
            return BigDecimal.ZERO;
        }
        return best.getLast().value().divide(best.getFirst().value(), PRECISION).subtract(BigDecimal.ONE).multiply(HUNDRED);
    }

    private static int weeks(List<Figure> figures) {
        return (int) figures.stream().map(Figure::weekStart).distinct().count();
    }

    private static BigDecimal sum(List<Figure> figures) {
        return figures.stream().map(Figure::value).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String wholePercent(BigDecimal fraction) {
        return fraction.multiply(HUNDRED).setScale(0, RoundingMode.HALF_UP).abs().toPlainString();
    }

    private static String decimal(boolean polish, BigDecimal value) {
        String text = value.setScale(1, RoundingMode.HALF_UP).toPlainString();
        return polish ? text.replace('.', ',') : text;
    }

    private static String joined(boolean polish, List<String> values) {
        if (values.size() <= 1) {
            return String.join("", values);
        }
        return String.join(", ", values.subList(0, values.size() - 1)) + (polish ? " i " : " and ") + values.getLast();
    }

    /** The Polish form of a noun after {@code count}: 1, 2 to 4 (but not 12 to 14), or any other number. */
    static String plural(int count, String one, String few, String many) {
        if (count == 1) {
            return one;
        }
        int lastDigit = count % 10;
        int lastTwo = count % 100;
        return lastDigit >= 2 && lastDigit <= 4 && (lastTwo < 12 || lastTwo > 14) ? few : many;
    }
}
