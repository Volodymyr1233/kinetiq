package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionSummary;
import java.util.List;
import java.util.Objects;

/**
 * A finished session log with its summary, which is worked out from the log as it is now, its PRs in the order of the
 * session's exercises, and the client's week streak.
 */
public record SessionSummaryView(SessionLog log, SessionSummary summary, List<ExercisePr> prs, int weeksInARow) {

    public SessionSummaryView {
        prs = List.copyOf(prs);
    }

    /** One PR of the session: the exercise from the trainer's library and its top set in the session. */
    public record ExercisePr(Exercise exercise, TopSet top) {

        public ExercisePr {
            Objects.requireNonNull(exercise);
            Objects.requireNonNull(top);
        }
    }
}
