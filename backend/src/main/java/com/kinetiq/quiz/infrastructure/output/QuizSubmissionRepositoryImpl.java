package com.kinetiq.quiz.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class QuizSubmissionRepositoryImpl implements QuizSubmissionRepository {

    private static final String UNIQUE_CLIENT = "uq_quiz_submissions_client";

    private final JpaQuizSubmissionRepository jpa;
    private final QuizSubmissionMapper mapper;

    @Override
    public boolean existsByClient(AccountId client) {
        return jpa.existsByClientId(client.value());
    }

    // flushed at once, so a submit that lost a race on the client's unique constraint fails here, where it can be told
    // apart from other failures
    @Override
    public void save(QuizSubmission submission) {
        try {
            jpa.saveAndFlush(mapper.toEntity(submission));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && UNIQUE_CLIENT.equals(violation.getConstraintName())) {
                throw new QuizAlreadySubmittedException();
            }
            throw exception;
        }
    }
}
