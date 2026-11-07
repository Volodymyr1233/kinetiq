package com.kinetiq.client.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.client.domain.input.ReviewClientAnswersUseCase;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.quiz.domain.exception.QuizNotFoundException;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ClientAnswersReviewService implements ReviewClientAnswersUseCase {

    private final ClientDirectory clients;
    private final ViewQuizAnswersUseCase quiz;

    @Override
    public QuizSubmission answers(AccountId trainer, AccountId client) {
        if (clients.findClient(trainer, client).isEmpty()) {
            throw new ClientNotFoundException();
        }
        return quiz.answers(client).orElseThrow(QuizNotFoundException::new);
    }
}
