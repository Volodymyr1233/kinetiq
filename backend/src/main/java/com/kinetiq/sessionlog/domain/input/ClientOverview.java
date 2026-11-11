package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.readiness.domain.model.Cause;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * What a trainer first reads of one client: {@code today} is the client's local date, the week is the client's current
 * plan week, absent when no plan is published or the plan has finished, the recent sessions are the newest that were
 * started or have passed, and the quiz answers are absent before the quiz. {@code readiness} holds today and the 13 days
 * before it, oldest first, and {@code recoveryDays} each of those days in the Recovery day band, newest first.
 */
@Getter
public class ClientOverview {

    private final LocalDate today;

    @Getter(AccessLevel.NONE)
    private final Week week;

    private final List<ClientSession> recentSessions;

    @Getter(AccessLevel.NONE)
    private final QuizSubmission quizAnswers;

    private final List<ReadinessDay> readiness;
    private final List<RecoveryDay> recoveryDays;

    /** {@code week} and {@code quizAnswers} may be {@code null}, as the class says. */
    public ClientOverview(
            LocalDate today,
            Week week,
            List<ClientSession> recentSessions,
            QuizSubmission quizAnswers,
            List<ReadinessDay> readiness,
            List<RecoveryDay> recoveryDays) {
        this.today = Objects.requireNonNull(today);
        this.week = week;
        this.recentSessions = List.copyOf(recentSessions);
        this.quizAnswers = quizAnswers;
        this.readiness = List.copyOf(readiness);
        this.recoveryDays = List.copyOf(recoveryDays);
    }

    public Optional<Week> getWeek() {
        return Optional.ofNullable(week);
    }

    public Optional<QuizSubmission> getQuizAnswers() {
        return Optional.ofNullable(quizAnswers);
    }

    /** A plan week: its number, the Monday it starts on, and its planned sessions in week order. */
    public record Week(int number, LocalDate startsOn, List<ClientSession> sessions) {

        public Week {
            Objects.requireNonNull(startsOn);
            sessions = List.copyOf(sessions);
        }
    }

    /** One day of the last 14: its readiness, absent without a check-in that day, never a score of 0 in its place. */
    @Getter
    public static class ReadinessDay {

        private final LocalDate date;

        @Getter(AccessLevel.NONE)
        private final Readiness readiness;

        /** {@code readiness} is {@code null} without a check-in. */
        public ReadinessDay(LocalDate date, Readiness readiness) {
            this.date = Objects.requireNonNull(date);
            this.readiness = readiness;
        }

        public Optional<Readiness> getReadiness() {
            return Optional.ofNullable(readiness);
        }
    }

    /**
     * A day of the last 14 whose check-in is in the Recovery day band, with the answers that made it one. {@code targets}
     * is {@code LIGHTER} when the day's adjustment is in use, {@code ORIGINAL} when the client chose the original
     * targets, and absent when nothing could be lighter. {@code session} is the session started that day, else the
     * plan's session of that weekday within the plan's dates, else absent.
     */
    @Getter
    public static class RecoveryDay {

        private final LocalDate date;
        private final int score;
        private final List<Cause> causes;

        @Getter(AccessLevel.NONE)
        private final TargetChoice targets;

        @Getter(AccessLevel.NONE)
        private final ClientSession session;

        /** {@code targets} and {@code session} may be {@code null}, as the class says. */
        public RecoveryDay(LocalDate date, int score, List<Cause> causes, TargetChoice targets, ClientSession session) {
            this.date = Objects.requireNonNull(date);
            this.score = score;
            this.causes = List.copyOf(causes);
            this.targets = targets;
            this.session = session;
        }

        public Optional<TargetChoice> getTargets() {
            return Optional.ofNullable(targets);
        }

        public Optional<ClientSession> getSession() {
            return Optional.ofNullable(session);
        }
    }
}
