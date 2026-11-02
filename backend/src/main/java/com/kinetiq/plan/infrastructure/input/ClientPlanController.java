package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/plan/** needs the role CLIENT; the plan is the signed-in client's own, so no id is taken from the request
@RestController
@RequestMapping("/api/v1/plan")
@RequiredArgsConstructor
class ClientPlanController {

    private final ViewOwnPlanUseCase plans;
    private final PlanWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * The signed-in client's published plan for plan week {@code week}, the current one by default, with exercise names
     * in the language of {@code Accept-Language}. {@code 404} with {@code plan.not-found} for no plan and for a draft
     * alike, {@code 400} with {@code plan.week-out-of-range} for a week the plan does not have.
     */
    @GetMapping
    ClientPlanResponse getOwnPlan(
            @RequestParam(required = false) Integer week, Locale locale, Authentication authentication) {
        return mapper.toResponse(plans.view(signedIn(authentication), week), locale);
    }

    /** Marks the plan as seen; the first call changes it and later ones do not. */
    @PostMapping("/acknowledgement")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void acknowledgePlan(Authentication authentication) {
        plans.acknowledge(signedIn(authentication));
    }
}
