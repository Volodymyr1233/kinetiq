package com.kinetiq.client.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ReviewClientAnswersUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/clients/** needs the role TRAINER; the use case checks the client is this trainer's own
@RestController
@RequiredArgsConstructor
class ClientQuizAnswersController {

    private final ReviewClientAnswersUseCase answers;
    private final ClientWebMapper mapper;

    /**
     * The quiz answers of one of the signed-in trainer's clients. {@code 404} with {@code client.not-found} for an
     * unknown client and another trainer's alike, and with {@code quiz.not-found} for an own client with no answers.
     */
    @GetMapping("/api/v1/clients/{id}/quiz-answers")
    QuizAnswersResponse quizAnswers(@PathVariable UUID id, Authentication authentication) {
        // sign-in stores the account id as the principal's name
        AccountId trainer = new AccountId(UUID.fromString(authentication.getName()));
        return mapper.toResponse(answers.answers(trainer, new AccountId(id)));
    }
}
