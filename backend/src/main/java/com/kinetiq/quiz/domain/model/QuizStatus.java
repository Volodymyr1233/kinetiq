package com.kinetiq.quiz.domain.model;

/** Where the signed-in client stands with the quiz. {@code PLAN_PUBLISHED} is supplied by the plan stories. */
public enum QuizStatus {
    NOT_SUBMITTED,
    WAITING_FOR_PLAN,
    PLAN_PUBLISHED
}
