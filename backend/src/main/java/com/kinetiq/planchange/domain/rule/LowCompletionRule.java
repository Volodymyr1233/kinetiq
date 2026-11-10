package com.kinetiq.planchange.domain.rule;

import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.Finding;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.WeekTraining;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** Fires when fewer than 70% of the planned sessions were finished; never for a plan of the smallest size. */
public final class LowCompletionRule implements PlanChangeRule {

    // ADR 0002: a plan never goes below two sessions a week, so there is nothing left to remove from one
    private static final int SMALLEST_PLAN = 2;

    @Override
    public Rule rule() {
        return Rule.LOW_COMPLETION;
    }

    @Override
    public Optional<Finding> evaluate(List<WeekTraining> window) {
        if (window.stream().anyMatch(week -> week.sessionsInPlan() <= SMALLEST_PLAN)) {
            return Optional.empty();
        }
        long planned = window.stream().mapToLong(WeekTraining::plannedSessions).sum();
        long finished = window.stream().mapToLong(WeekTraining::finishedSessions).sum();
        // finished / planned < 70% without a division
        return planned > 0 && finished * 10 < planned * 7
                ? Optional.of(new Finding(Rule.LOW_COMPLETION, Set.of(), figures(window)))
                : Optional.empty();
    }

    private static List<Figure> figures(List<WeekTraining> window) {
        return window.stream()
                .flatMap(week -> List.of(
                        Figure.ofWeek(
                                FigureKind.SESSIONS_PLANNED, week.monday(), BigDecimal.valueOf(week.plannedSessions())),
                        Figure.ofWeek(
                                FigureKind.SESSIONS_FINISHED,
                                week.monday(),
                                BigDecimal.valueOf(week.finishedSessions())))
                        .stream())
                .toList();
    }
}
