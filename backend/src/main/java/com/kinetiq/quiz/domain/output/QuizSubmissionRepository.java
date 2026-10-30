package com.kinetiq.quiz.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.QuizSubmission;

public interface QuizSubmissionRepository {

    /** Tells whether the client has a stored submission. */
    boolean existsByClient(AccountId client);

    /** Stores a new submission. Throws {@code QuizAlreadySubmittedException} when the client already has one. */
    void save(QuizSubmission submission);
}
