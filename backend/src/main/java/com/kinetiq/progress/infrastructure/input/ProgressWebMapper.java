package com.kinetiq.progress.infrastructure.input;

import com.kinetiq.progress.domain.input.Progress;
import com.kinetiq.progress.domain.input.Progress.ExerciseProgress;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.progress.domain.model.TrainingWeek;
import com.kinetiq.progress.infrastructure.input.ProgressResponse.ExerciseProgressResponse;
import com.kinetiq.progress.infrastructure.input.ProgressResponse.ExerciseWeekResponse;
import com.kinetiq.progress.infrastructure.input.ProgressResponse.ProgressWeekResponse;
import com.kinetiq.progress.infrastructure.input.ProgressResponse.RecentSetResponse;
import com.kinetiq.progress.infrastructure.input.ProgressResponse.TopSetChangeResponse;
import com.kinetiq.progress.infrastructure.input.ProgressResponse.TopSetResponse;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
class ProgressWebMapper {

    ProgressResponse toResponse(Progress progress, Locale locale) {
        return new ProgressResponse(
                progress.getGoal(),
                progress.getWeek(),
                progress.isFinished(),
                progress.getSessionsDone(),
                progress.getSessionsPlanned(),
                progress.getSessionsDoneChange().orElse(null),
                progress.getAverageRpe().orElse(null),
                progress.getAverageRpeChange().orElse(null),
                progress.getWeeksInARow(),
                progress.getWeeks().stream().map(ProgressWebMapper::week).toList(),
                progress.getExercises().stream()
                        .map(exercise -> exercise(exercise, locale))
                        .toList());
    }

    private static ProgressWeekResponse week(TrainingWeek week) {
        return new ProgressWeekResponse(
                week.number(),
                week.monday(),
                week.planned(),
                week.done(),
                week.running(),
                week.averageRpe().orElse(null));
    }

    private static ExerciseProgressResponse exercise(ExerciseProgress progress, Locale locale) {
        return new ExerciseProgressResponse(
                progress.getExercise().getId().value(),
                progress.getExercise().getName().in(locale),
                progress.getExercise().getMeasure(),
                top(progress.getBest()),
                progress.getTopSetChange()
                        .map(change -> new TopSetChangeResponse(change.loadKg(), change.amount()))
                        .orElse(null),
                progress.isPrToday(),
                progress.getWeeks().stream()
                        .map(week -> new ExerciseWeekResponse(week.number(), top(week.top()), week.averageRpe()))
                        .toList(),
                progress.getRecent().stream()
                        .map(recent -> new RecentSetResponse(recent.date(), top(recent.top())))
                        .toList());
    }

    private static TopSetResponse top(TopSet set) {
        return new TopSetResponse(set.loadKg(), set.amount(), set.rpe());
    }
}
