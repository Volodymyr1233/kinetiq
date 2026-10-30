package com.kinetiq.quiz.infrastructure.input;

import com.kinetiq.quiz.domain.model.Experience;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.quiz.domain.model.Level;
import com.kinetiq.quiz.domain.model.Like;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.Set;

/** The seven answers; the client is the signed-in one, so there is no field for it. A like sent twice counts once. */
record SubmitQuizRequest(
        @NotNull Goal goal,
        @NotNull Experience experience,
        @NotNull Level level,
        @Min(QuizSubmission.MIN_AGE) @Max(QuizSubmission.MAX_AGE) int age,
        @Min(QuizSubmission.MIN_HEIGHT_CM) @Max(QuizSubmission.MAX_HEIGHT_CM) int heightCm,
        @NotNull @DecimalMin("30.0") @DecimalMax("250.0") @Digits(integer = 3, fraction = 1) BigDecimal weightKg,
        @NotEmpty Set<Like> likes,
        @Min(QuizSubmission.MIN_DAYS_PER_WEEK) @Max(QuizSubmission.MAX_DAYS_PER_WEEK) int daysPerWeek,
        @Size(max = QuizSubmission.NOTES_MAX_LENGTH) String notes) {}
