package com.kinetiq.quiz.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.input.SubmitQuizUseCase;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// always the signed-in client's own quiz, so the address carries no id
@RestController
@RequestMapping("/api/v1/quiz")
@RequiredArgsConstructor
class QuizController {

    private final SubmitQuizUseCase quiz;
    private final QuizWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /** Answers whether the client has submitted the quiz and whether a plan is published. */
    @GetMapping
    QuizStatusResponse status(Authentication authentication) {
        return new QuizStatusResponse(quiz.status(signedIn(authentication)));
    }

    /** Submits the client's answers once. Refused with {@code 409} when the client has submitted before. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    QuizStatusResponse submit(@Valid @RequestBody SubmitQuizRequest request, Authentication authentication) {
        return new QuizStatusResponse(quiz.submit(mapper.toCommand(signedIn(authentication), request)));
    }
}
