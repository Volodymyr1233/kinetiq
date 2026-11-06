package com.kinetiq.quiz.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.util.Optional;

public interface ViewQuizAnswersUseCase {

    /** The client's submitted answers, empty when the client has not submitted the quiz. */
    Optional<QuizSubmission> answers(AccountId client);
}
