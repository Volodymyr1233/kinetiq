package com.kinetiq.quiz.domain.model;

/** Where the signed-in client stands with the quiz and the plan that follows it. */
public enum QuizStatus {
    NOT_SUBMITTED,
    WAITING_FOR_PLAN,
    PLAN_PUBLISHED
}
