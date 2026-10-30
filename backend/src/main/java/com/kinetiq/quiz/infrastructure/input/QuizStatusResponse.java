package com.kinetiq.quiz.infrastructure.input;

import com.kinetiq.quiz.domain.model.QuizStatus;
import jakarta.validation.constraints.NotNull;

// @NotNull marks the fields required in the OpenAPI document; responses are never validated
record QuizStatusResponse(@NotNull QuizStatus status) {}
