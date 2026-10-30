package com.kinetiq.quiz.domain.model;

import java.util.Objects;
import java.util.UUID;

public record QuizSubmissionId(UUID value) {

    public QuizSubmissionId {
        Objects.requireNonNull(value);
    }

    public static QuizSubmissionId random() {
        return new QuizSubmissionId(UUID.randomUUID());
    }
}
