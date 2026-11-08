package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.Getter;

/**
 * What a trainer first reads of one client: {@code today} is the client's local date, the week is the client's current
 * plan week, absent when no plan is published or the plan has finished, the recent sessions are the newest that were
 * started or have passed, and the quiz answers are absent before the quiz.
 */
@Getter
public class ClientOverview {

    private final LocalDate today;

    @Getter(AccessLevel.NONE)
    private final Week week;

    private final List<ClientSession> recentSessions;

    @Getter(AccessLevel.NONE)
    private final QuizSubmission quizAnswers;

    /** {@code week} and {@code quizAnswers} may be {@code null}, as the class says. */
    public ClientOverview(
            LocalDate today, Week week, List<ClientSession> recentSessions, QuizSubmission quizAnswers) {
        this.today = Objects.requireNonNull(today);
        this.week = week;
        this.recentSessions = List.copyOf(recentSessions);
        this.quizAnswers = quizAnswers;
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
}
