package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DecideOwnPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// every path needs the role CLIENT (SecurityConfig); the use case checks the plan change is this client's own
@RestController
@RequiredArgsConstructor
class ClientPlanChangeController {

    private final DecideOwnPlanChangeUseCase decisions;
    private final PlanChangeWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * The client accepts. Accepting twice answers the unchanged plan change; {@code 404} with {@code
     * plan-change.not-found} for an unknown one and another client's alike, {@code 409} when it has ended or its
     * effective week has begun.
     */
    @PostMapping("/api/v1/plan/changes/{id}/acceptance")
    ClientPlanChangeDetailResponse accept(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(decisions.accept(signedIn(authentication), new PlanChangeId(id)));
    }

    /** The client takes their acceptance back; {@code 409} when they have none to take back or it has ended. */
    @DeleteMapping("/api/v1/plan/changes/{id}/acceptance")
    ClientPlanChangeDetailResponse undo(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(decisions.undo(signedIn(authentication), new PlanChangeId(id)));
    }
}
