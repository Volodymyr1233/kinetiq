package com.kinetiq.sessionlog.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.input.ViewSessionStatesUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/session-states/** needs the role CLIENT; the states are the signed-in client's own
@RestController
@RequestMapping("/api/v1/session-states")
@RequiredArgsConstructor
class SessionStateController {

    private final ViewSessionStatesUseCase states;
    private final SessionLogWebMapper mapper;

    /**
     * Where each planned session of plan week {@code week} stands, or of the current plan week without it; a finished
     * plan asked without a week has none. {@code 404} with {@code plan.not-found} for no plan and for a draft alike,
     * and {@code 400} with {@code plan.week-out-of-range} for a week the plan does not have.
     */
    @GetMapping
    SessionStatesResponse getStates(@RequestParam(required = false) Integer week, Authentication authentication) {
        // sign-in stores the account id as the principal's name
        return mapper.toResponse(states.states(new AccountId(UUID.fromString(authentication.getName())), week));
    }
}
