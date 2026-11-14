package com.kinetiq.progress.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.input.OwnPlanView;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.progress.domain.input.Progress;
import com.kinetiq.progress.domain.input.Progress.ExerciseProgress;
import com.kinetiq.progress.domain.input.Progress.TopSetChange;
import com.kinetiq.progress.domain.input.Rewards;
import com.kinetiq.progress.domain.input.ViewProgressUseCase;
import com.kinetiq.progress.domain.model.LoggedExercise;
import com.kinetiq.progress.domain.model.Period;
import com.kinetiq.progress.domain.model.Pr;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.progress.domain.model.TrainingWeek;
import com.kinetiq.progress.domain.model.TrainingWeek.ExerciseWeek;
import com.kinetiq.progress.domain.output.TrainingLog;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ProgressViewService implements ViewProgressUseCase {

    private static final int RECENT_SESSIONS = 3;
    private static final int SESSIONS_IN_A_ROW = 2;
    private static final int DAYS_PER_WEEK = 7;

    // the most sessions first, then the newest; a full tie still gets one order, so two reads agree
    private static final Comparator<List<LoggedExercise>> EXERCISE_ORDER =
            Comparator.<List<LoggedExercise>>comparingInt(List::size)
                    .reversed()
                    .thenComparing(rows -> rows.getLast().date(), Comparator.reverseOrder())
                    .thenComparing(rows -> rows.getFirst().exercise().value());

    private final ViewClientsUseCase clients;
    private final ViewOwnPlanUseCase plans;
    private final CurrentAccountUseCase accounts;
    private final TrainingLog trainingLog;
    private final Clock clock;

    @Override
    public Progress ofClient(AccountId trainer, AccountId client, Period period) {
        clients.getClient(trainer, client);
        return own(client, period);
    }

    @Override
    public Progress own(AccountId client, Period period) {
        Training training = training(client);
        OwnPlanView view = training.view();
        List<LoggedExercise> logged = training.logged();
        List<TrainingWeek> all = training.weeks();
        List<TrainingWeek> weeks = all.subList(Math.max(0, all.size() - period.weeks()), all.size());
        Optional<Compared> compared = compared(weeks);
        LocalDate today = LocalDate.now(clock.withZone(accounts.find(client).getTimeZone()));
        return new Progress(
                view.plan().getGoal(),
                training.lastWeek(),
                view.finished(),
                weeks,
                weeks.stream().mapToInt(TrainingWeek::done).sum(),
                weeks.stream().mapToInt(TrainingWeek::planned).sum(),
                TrainingWeek.averageRpe(weeks).orElse(null),
                compared.filter(pair -> pair.earlier().planned() > 0 && pair.latest().planned() > 0)
                        .map(pair -> pair.latest().done() - pair.earlier().done())
                        .orElse(null),
                compared.flatMap(pair -> pair.latest()
                                .averageRpe()
                                .flatMap(latest -> pair.earlier().averageRpe().map(latest::subtract)))
                        .orElse(null),
                weeksInARow(all),
                exercises(weeks, compared, logged, view.library(), today));
    }

    @Override
    public Rewards rewards(AccountId client, LocalDate date) {
        Training training = training(client);
        return new Rewards(Pr.on(date, training.logged()), weeksInARow(training.weeks()));
    }

    // own and rewards read the same weeks and rows, so every screen shows the same streak and PRs
    private Training training(AccountId client) {
        OwnPlanView view = plans.view(client, null);
        Plan plan = view.plan();
        int lastWeek = view.finished() ? plan.getWeeks() : view.currentWeek();
        List<LoggedExercise> logged = trainingLog.loggedExercises(client);
        return new Training(
                view,
                lastWeek,
                logged,
                TrainingWeek.of(plan, lastWeek, !view.finished(), logged, trainingLog.finishedDays(client)));
    }

    // the last week of the period that is not running and the week before it; only the last week can be running
    private static Optional<Compared> compared(List<TrainingWeek> weeks) {
        List<TrainingWeek> completed = weeks.getLast().running() ? weeks.subList(0, weeks.size() - 1) : weeks;
        if (completed.size() < 2) {
            return Optional.empty();
        }
        return Optional.of(new Compared(completed.get(completed.size() - 2), completed.getLast()));
    }

    // a running week that does not count yet cannot end the streak before it ends
    private static int weeksInARow(List<TrainingWeek> weeks) {
        int streak = 0;
        for (TrainingWeek week : weeks.reversed()) {
            if (counts(week)) {
                streak++;
            } else if (!week.running()) {
                break;
            }
        }
        return streak;
    }

    // a week without a planned session (a first week published after its last session day) does not count
    private static boolean counts(TrainingWeek week) {
        return week.planned() > 0 && week.done() >= Math.min(SESSIONS_IN_A_ROW, week.planned());
    }

    private static List<ExerciseProgress> exercises(
            List<TrainingWeek> weeks,
            Optional<Compared> compared,
            List<LoggedExercise> logged,
            Map<ExerciseId, Exercise> library,
            LocalDate today) {
        LocalDate first = weeks.getFirst().monday();
        LocalDate last = weeks.getLast().monday().plusDays(DAYS_PER_WEEK - 1L);
        Map<ExerciseId, List<LoggedExercise>> allRows =
                logged.stream().collect(Collectors.groupingBy(LoggedExercise::exercise));
        return logged.stream()
                .filter(row -> !row.date().isBefore(first) && !row.date().isAfter(last))
                .collect(Collectors.groupingBy(LoggedExercise::exercise))
                .values()
                .stream()
                .sorted(EXERCISE_ORDER)
                .map(inPeriod -> {
                    ExerciseId id = inPeriod.getFirst().exercise();
                    List<LoggedExercise> rows = allRows.get(id);
                    return new ExerciseProgress(
                            library.get(id),
                            best(inPeriod).top(),
                            compared.flatMap(pair -> topSetChange(pair, id)).orElse(null),
                            best(rows).date().equals(today),
                            weeks.stream()
                                    .filter(week -> week.exercises().containsKey(id))
                                    .map(week -> {
                                        ExerciseWeek exercise = week.exercises().get(id);
                                        return new ExerciseProgress.Week(
                                                week.number(), exercise.top(), exercise.averageRpe());
                                    })
                                    .toList(),
                            rows.reversed().stream()
                                    .limit(RECENT_SESSIONS)
                                    .map(row -> new ExerciseProgress.Recent(row.date(), row.top()))
                                    .toList());
                })
                .toList();
    }

    // rows are oldest first, so of two equal top sets the earlier stays
    private static LoggedExercise best(List<LoggedExercise> rows) {
        return rows.stream()
                .reduce((best, row) -> row.top().isBetterThan(best.top()) ? row : best)
                .orElseThrow();
    }

    // a week with a load and one without are not compared
    private static Optional<TopSetChange> topSetChange(Compared pair, ExerciseId id) {
        ExerciseWeek earlier = pair.earlier().exercises().get(id);
        ExerciseWeek latest = pair.latest().exercises().get(id);
        if (earlier == null || latest == null) {
            return Optional.empty();
        }
        TopSet before = earlier.top();
        TopSet after = latest.top();
        if (before.loadKg() != null && after.loadKg() != null) {
            return Optional.of(new TopSetChange(after.loadKg().subtract(before.loadKg()), null));
        }
        if (before.loadKg() == null && after.loadKg() == null) {
            return Optional.of(new TopSetChange(null, after.amount() - before.amount()));
        }
        return Optional.empty();
    }

    private record Compared(TrainingWeek earlier, TrainingWeek latest) {}

    private record Training(OwnPlanView view, int lastWeek, List<LoggedExercise> logged, List<TrainingWeek> weeks) {}
}
