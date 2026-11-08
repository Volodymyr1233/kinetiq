package com.kinetiq.sessionlog.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.input.ViewClientTrainingUseCase;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import java.util.Locale;
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
        return mapper.toResponse(training.overview(trainer(authentication), new AccountId(id)));
    }

    /**
     * Every session of one of the signed-in trainer's clients, newest first. {@code 404} with {@code client.not-found}
     * for an unknown client and another trainer's alike.
     */
    @GetMapping("/api/v1/clients/{id}/session-logs")
    ClientSessionsResponse sessions(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(training.sessions(trainer(authentication), new AccountId(id)));
    }

    /**
     * One session log of one of the signed-in trainer's clients, each planned set with its target and its log, the
     * exercise names in the request's language. {@code 404} with {@code client.not-found} for an unknown client and
     * another trainer's alike, and with {@code session-log.not-found} for a log that is not this client's.
     */
    @GetMapping("/api/v1/clients/{id}/session-logs/{sessionLogId}")
    ClientSessionLogResponse sessionLog(
            @PathVariable UUID id, @PathVariable UUID sessionLogId, Locale locale, Authentication authentication) {
        return mapper.toResponse(
                training.sessionLog(trainer(authentication), new AccountId(id), new SessionLogId(sessionLogId)),
                locale);
    }

    // sign-in stores the account id as the principal's name
    private static AccountId trainer(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }
}
