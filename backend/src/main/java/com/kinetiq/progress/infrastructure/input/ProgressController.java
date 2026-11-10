package com.kinetiq.progress.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.progress.domain.input.ViewProgressUseCase;
import com.kinetiq.progress.domain.model.Period;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/progress needs the role CLIENT and /api/v1/clients/** the role TRAINER; the use case checks the client is the
// trainer's own
@RestController
@RequiredArgsConstructor
class ProgressController {

    private final ViewProgressUseCase progress;
    private final ProgressWebMapper mapper;

    /**
     * The signed-in client's progress over the last {@code weeks} plan weeks, exercise names in the request's language.
     * {@code 400} with {@code progress.period-invalid} for anything but 4, 8 or 12 weeks, and {@code 404} with
     * {@code plan.not-found} without a published plan.
     */
    @GetMapping("/api/v1/progress")
    ProgressResponse own(@RequestParam int weeks, Locale locale, Authentication authentication) {
        return mapper.toResponse(progress.own(account(authentication), new Period(weeks)), locale);
    }

    /**
     * The same for one of the signed-in trainer's clients. {@code 400} with {@code progress.period-invalid} for
     * anything but 4, 8 or 12 weeks, {@code 404} with {@code client.not-found} for an unknown client and another
     * trainer's alike, and with {@code plan.not-found} for a client without a published plan.
     */
    @GetMapping("/api/v1/clients/{id}/progress")
    ProgressResponse ofClient(
            @PathVariable UUID id, @RequestParam int weeks, Locale locale, Authentication authentication) {
        return mapper.toResponse(
                progress.ofClient(account(authentication), new AccountId(id), new Period(weeks)), locale);
    }

    // sign-in stores the account id as the principal's name
    private static AccountId account(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }
}
