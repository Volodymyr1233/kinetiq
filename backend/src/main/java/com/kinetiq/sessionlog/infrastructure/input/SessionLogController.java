package com.kinetiq.sessionlog.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.input.LogSessionUseCase;
import com.kinetiq.sessionlog.domain.input.LogSetCommand;
import com.kinetiq.sessionlog.domain.input.StartedSession;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/session-logs/** needs the role CLIENT; the session is the signed-in client's own, so no client id is taken
// from the request
@RestController
@RequestMapping("/api/v1/session-logs")
@RequiredArgsConstructor
class SessionLogController {

    private final LogSessionUseCase sessions;
    private final SessionLogWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * Starts the session the client's plan has today, or returns the one started already: {@code 201} when it was
     * created and {@code 200} when it existed. {@code 404} with {@code plan.not-found} for no plan and for a draft
     * alike, {@code 409} with {@code session-log.no-session-today} on a rest day and for a finished plan.
     */
    @PostMapping
    ResponseEntity<SessionLogResponse> start(Locale locale, Authentication authentication) {
        StartedSession started = sessions.start(signedIn(authentication));
        return ResponseEntity.status(started.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(mapper.toResponse(started, locale));
    }

    /**
     * Logs a set, or changes it: the first call with an id creates it, and a repeat with the same values changes
     * nothing. {@code 404} with {@code session-log.not-found} for a session that is not the client's, {@code 400} with
     * {@code set-values.invalid} for a value outside its limits, {@code 409} when the id or the place is already used.
     */
    @PutMapping("/{id}/set-logs/{setLogId}")
    SetLogResponse logSet(
            @PathVariable UUID id,
            @PathVariable UUID setLogId,
            @Valid @RequestBody LogSetRequest request,
            Authentication authentication) {
        return mapper.toResponse(sessions.logSet(new LogSetCommand(
                signedIn(authentication),
                new SessionLogId(id),
                new SetLogId(setLogId),
                request.entryPosition(),
                request.setNumber(),
                request.loadKg(),
                request.amount(),
                request.rpe())));
    }
}
