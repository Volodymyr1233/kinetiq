package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

// both paths need the role TRAINER (SecurityConfig); the use case checks the client is this trainer's own
@RestController
@RequiredArgsConstructor
class PlanChangeController {

    private final ViewPlanChangeUseCase planChanges;
    private final PlanChangeWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * A plan change of one of the trainer's own clients. {@code 404} with {@code plan-change.not-found} for an unknown
     * one and another trainer's client's alike.
     */
    @GetMapping("/api/v1/plan-changes/{id}")
    PlanChangeResponse getPlanChange(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(planChanges.get(signedIn(authentication), new PlanChangeId(id)));
    }

    /**
     * The open plan change of the trainer's own client. {@code 404} with {@code client.not-found} for another trainer's
     * client, and with {@code plan-change.not-found} when the client has none open.
     */
    @GetMapping("/api/v1/clients/{id}/plan-change")
    ClientPlanChangeResponse getOpenPlanChange(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(planChanges.openOf(signedIn(authentication), new AccountId(id)));
    }
}
