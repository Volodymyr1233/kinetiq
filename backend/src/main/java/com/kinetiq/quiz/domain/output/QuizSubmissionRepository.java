package com.kinetiq.quiz.domain.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.util.Optional;

public interface QuizSubmissionRepository {

    /** Tells whether the client has a stored submission. */
    boolean existsByClient(AccountId client);

    /** Finds the client's submission. */
    Optional<QuizSubmission> findByClient(AccountId client);

    /** Tells whether the client has a published plan. */
    boolean hasPublishedPlan(AccountId client);

    /** Stores a new submission. Throws {@code QuizAlreadySubmittedException} when the client already has one. */
    void save(QuizSubmission submission);
}
