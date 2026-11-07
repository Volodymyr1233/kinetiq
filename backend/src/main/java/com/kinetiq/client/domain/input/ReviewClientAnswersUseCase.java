package com.kinetiq.client.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.QuizSubmission;

public interface ReviewClientAnswersUseCase {

    /**
     * The quiz answers of one of the trainer's own clients. {@code ClientNotFoundException} for any other account,
     * {@code QuizNotFoundException} for an own client with no answers.
     */
    QuizSubmission answers(AccountId trainer, AccountId client);
}
