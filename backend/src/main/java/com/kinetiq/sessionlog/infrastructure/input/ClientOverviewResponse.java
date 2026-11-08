package com.kinetiq.sessionlog.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
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
 * {@code quizAnswers} before the quiz.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ClientOverviewResponse(
        @NotNull LocalDate today,
        OverviewWeekResponse week,
        @NotNull List<ClientSessionResponse> recentSessions,
        OverviewQuizAnswersResponse quizAnswers) {

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
}
