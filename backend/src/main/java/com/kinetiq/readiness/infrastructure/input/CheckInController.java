package com.kinetiq.readiness.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.input.SaveCheckInCommand;
import com.kinetiq.readiness.domain.model.Answers;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/check-ins/** needs the role CLIENT; the client and the day come from the sign-in, so no id or date is taken
// from the request
@RestController
@RequestMapping("/api/v1/check-ins/today")
@RequiredArgsConstructor
class CheckInController {

    private final CheckInUseCase checkIns;
    private final CheckInWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * The signed-in client's check-in today in their time zone, if any, and whether one can still be saved; a rest day
     * and a finished plan read {@code open: false}. {@code 404} with {@code plan.not-found} for no plan and for a draft
     * alike.
     */
    @GetMapping
    TodayCheckInResponse getTodayCheckIn(Authentication authentication) {
        return mapper.toResponse(checkIns.today(signedIn(authentication)));
    }

    /**
     * Saves today's check-in, or replaces the answers of the one saved today; a repeat gives the same result.
     * {@code 400} with {@code validation.failed} for an answer missing or outside 1 to 5, {@code 404} with
     * {@code plan.not-found} for no plan, {@code 409} with {@code check-in.no-session-today} on a rest day and for a
     * finished plan, and {@code 409} with {@code check-in.training-started} once a set of today's session is logged.
     */
    @PutMapping
    TodayCheckInResponse saveTodayCheckIn(
            @Valid @RequestBody SaveCheckInRequest request, Authentication authentication) {
        return mapper.toResponse(checkIns.save(new SaveCheckInCommand(
                signedIn(authentication), new Answers(request.sleep(), request.soreness(), request.stress()))));
    }
}
