package com.kinetiq.client.infrastructure.input;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
/** A client's quiz answers as the trainer reads them beside a draft; {@code notes} is left out when there are none. */
@JsonInclude(JsonInclude.Include.NON_NULL)
record QuizAnswersResponse(
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
