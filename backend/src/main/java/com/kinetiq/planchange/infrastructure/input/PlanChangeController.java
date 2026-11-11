package com.kinetiq.planchange.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.input.DecidePlanChangeUseCase;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

// both paths need the role TRAINER (SecurityConfig); the use case checks the client is this trainer's own
@RestController
@RequiredArgsConstructor
class PlanChangeController {

    private final ViewPlanChangeUseCase planChanges;
    private final DecidePlanChangeUseCase decisions;
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
     * The trainer accepts. Accepting twice answers the unchanged plan change; {@code 409} when it has ended or its
     * effective week has begun.
     */
    @PostMapping("/api/v1/plan-changes/{id}/acceptance")
    PlanChangeResponse accept(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(decisions.accept(signedIn(authentication), new PlanChangeId(id)));
    }

    /** The trainer takes their acceptance back; {@code 409} when they have none to take back or it has ended. */
    @DeleteMapping("/api/v1/plan-changes/{id}/acceptance")
    PlanChangeResponse undo(@PathVariable UUID id, Authentication authentication) {
        return mapper.toResponse(decisions.undo(signedIn(authentication), new PlanChangeId(id)));
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
