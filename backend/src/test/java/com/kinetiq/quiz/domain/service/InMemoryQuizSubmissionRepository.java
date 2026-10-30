package com.kinetiq.quiz.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import java.util.LinkedHashMap;
import java.util.Map;

class InMemoryQuizSubmissionRepository implements QuizSubmissionRepository {

    final Map<AccountId, QuizSubmission> byClient = new LinkedHashMap<>();

    @Override
    public boolean existsByClient(AccountId client) {
        return byClient.containsKey(client);
    }

    // honours the unique constraint on the client, as the database does
    @Override
    public void save(QuizSubmission submission) {
        if (byClient.putIfAbsent(submission.getClient(), submission) != null) {
            throw new QuizAlreadySubmittedException();
        }
    }
}
