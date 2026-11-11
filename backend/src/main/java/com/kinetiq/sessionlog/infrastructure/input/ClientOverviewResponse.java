package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.readiness.domain.model.Cause;
import com.kinetiq.readiness.domain.model.Readiness;
import com.kinetiq.readiness.domain.model.TargetChoice;
import com.kinetiq.sessionlog.domain.model.SessionState;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/**
 * One client's current plan week, recent sessions and quiz answers as their trainer reads them; {@code today} is the
 * client's local date. {@code week} is left out when no plan is published or the plan has finished, and
 * {@code quizAnswers} before the quiz. {@code readiness} is today and the 13 days before it, oldest first, and
 * {@code recoveryDays} each of those days in the Recovery day band, newest first.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ClientOverviewResponse(
        @NotNull LocalDate today,
        OverviewWeekResponse week,
        @NotNull List<ClientSessionResponse> recentSessions,
        OverviewQuizAnswersResponse quizAnswers,
        @NotNull List<ReadinessDayResponse> readiness,
        @NotNull List<RecoveryDayResponse> recoveryDays) {

    /** A plan week: its number, the Monday it starts on, and its planned sessions in week order. */
    record OverviewWeekResponse(
            @NotNull int number, @NotNull LocalDate startsOn, @NotNull List<ClientSessionResponse> sessions) {}

    /**
     * One session as the trainer reads it. {@code state} is left out before the plan's publish day, {@code averageRpe}
     * while no set is logged, and {@code sessionLogId} when the session was never started.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ClientSessionResponse(
            @NotNull LocalDate date,
            @NotNull DayOfWeek day,
            @NotNull String name,
            SessionState state,
            @NotNull boolean locked,
            @NotNull int setsLogged,
            @NotNull int setsPlanned,
            BigDecimal averageRpe,
            UUID sessionLogId) {}

    /** The client's quiz answers, as the trainer reads them beside a draft; {@code notes} is left out when empty. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record OverviewQuizAnswersResponse(
            @NotNull Goal goal,
            @NotNull Experience experience,
            @NotNull Level level,
            @NotNull int age,
            @NotNull int heightCm,
            @NotNull BigDecimal weightKg,
            @NotNull List<Like> likes,
            @NotNull int daysPerWeek,
            String notes,
            @NotNull Instant submittedAt) {}

    /** One day's readiness: {@code score} and {@code band} are both left out without a check-in, never 0 instead. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ReadinessDayResponse(@NotNull LocalDate date, Integer score, Readiness.Band band) {}

    /**
     * A recovery day: {@code targets} is left out when nothing could be lighter, and {@code session} when no session was
     * planned or started that day.
     */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    record RecoveryDayResponse(
            @NotNull LocalDate date,
            @NotNull int score,
            @NotNull List<Cause> causes,
            TargetChoice targets,
            ClientSessionResponse session) {}
}
