package com.kinetiq.overview.domain.model;

import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * What the trainer's overview reads of one client's training: the published plan's dates ({@code null} without a
 * published plan), the client's session logs by date, the newest logged set ({@code null} before the first) and the
 * check-ins by date. Logs and check-ins may cover only recent days.
 */
public record ClientTraining(
        PlanDates plan, Map<LocalDate, DayLog> days, LastLog lastLog, Map<LocalDate, DayCheckIn> checkIns) {

    private static final int COMPLETION_DAYS = 28;

    public ClientTraining {
        days = Map.copyOf(days);
        checkIns = Map.copyOf(checkIns);
    }

    /** One session log: whether it was finished, and the RPE of its sets. */
    public record DayLog(boolean finished, RpeTotal rpe) {

        public DayLog {
            Objects.requireNonNull(rpe);
        }
    }

    /** The newest logged set: when it was logged, and the date and name of its session. */
    public record LastLog(Instant at, LocalDate date, String sessionName) {

        public LastLog {
            Objects.requireNonNull(at);
            Objects.requireNonNull(date);
            Objects.requireNonNull(sessionName);
        }
    }

    /** One check-in: the answers, and which targets the client chose. */
    public record DayCheckIn(Answers answers, TargetChoice targets) {

        public DayCheckIn {
            Objects.requireNonNull(answers);
            Objects.requireNonNull(targets);
        }
    }

    /** Sessions planned and done in one week. */
    public record WeekSessions(int planned, int done) {}

    /** Whether the plan is published and has not finished on {@code today}. */
    public boolean running(LocalDate today) {
        return plan != null && !plan.finished(today);
    }

    /**
     * The sessions done of those due in the 28 days ending {@code today}, from the publish day on: a planned session is
     * due once its day has passed, or on its day once it is finished. Empty without a running plan or a due session.
     */
    public Optional<Completion> completion(LocalDate today) {
        if (!running(today)) {
            return Optional.empty();
        }
        int due = 0;
        int done = 0;
        for (LocalDate date = today.minusDays(COMPLETION_DAYS - 1L); !date.isAfter(today); date = date.plusDays(1)) {
            if (!plan.planned(date)) {
                continue;
            }
            boolean finished = finished(date);
            if (date.isBefore(today) || finished) {
                due++;
            }
            if (finished) {
                done++;
            }
        }
        return due == 0 ? Optional.empty() : Optional.of(new Completion(done, due));
    }

    /** The RPE of every set logged in session logs dated {@code from} to {@code to}, both included. */
    public RpeTotal rpe(LocalDate from, LocalDate to) {
        return days.entrySet().stream()
                .filter(day -> !day.getKey().isBefore(from) && !day.getKey().isAfter(to))
                .map(day -> day.getValue().rpe())
                .reduce(RpeTotal.NONE, RpeTotal::plus);
    }

    /**
     * The sessions planned and done in the Monday-to-Sunday week of {@code monday}; planned are the plan's sessions on
     * days of that week from the publish day, those still to come included. Nothing without a published plan.
     */
    public WeekSessions week(LocalDate monday) {
        if (plan == null) {
            return new WeekSessions(0, 0);
        }
        int planned = 0;
        int done = 0;
        for (LocalDate date = monday; date.isBefore(monday.plusWeeks(1)); date = date.plusDays(1)) {
            if (plan.planned(date)) {
                planned++;
                done += finished(date) ? 1 : 0;
            }
        }
        return new WeekSessions(planned, done);
    }

    private boolean finished(LocalDate date) {
        DayLog log = days.get(date);
        return log != null && log.finished();
    }
}
