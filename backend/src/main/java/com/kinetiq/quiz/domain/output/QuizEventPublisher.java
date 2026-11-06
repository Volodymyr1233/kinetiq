package com.kinetiq.quiz.domain.output;

import com.kinetiq.quiz.domain.model.QuizSubmitted;

/** Tells other features what happened to a quiz, without the quiz depending on them. */
public interface QuizEventPublisher {

    void submitted(QuizSubmitted event);
}
