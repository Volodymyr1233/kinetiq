package com.kinetiq.sessionlog.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.input.ViewClientTrainingUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/clients/** needs the role TRAINER; the use case checks the client is this trainer's own
@RestController
@RequiredArgsConstructor
class ClientTrainingController {

    private final ViewClientTrainingUseCase training;
    private final SessionLogWebMapper mapper;

    /**
     * The current plan week, the four most recent sessions and the quiz answers of one of the signed-in trainer's
     * clients. {@code 404} with {@code client.not-found} for an unknown client and another trainer's alike.
     */
    @GetMapping("/api/v1/clients/{id}/overview")
    ClientOverviewResponse overview(@PathVariable UUID id, Authentication authentication) {
        // sign-in stores the account id as the principal's name
        AccountId trainer = new AccountId(UUID.fromString(authentication.getName()));
        return mapper.toResponse(training.overview(trainer, new AccountId(id)));
    }
}
