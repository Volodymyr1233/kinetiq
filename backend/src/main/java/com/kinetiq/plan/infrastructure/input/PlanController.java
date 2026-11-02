package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.ManagePlanUseCase;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/clients/** needs the role TRAINER; the use case checks the client is this trainer's own
@RestController
@RequestMapping("/api/v1/clients/{id}/plan")
@RequiredArgsConstructor
class PlanController {

    private final ManagePlanUseCase plans;
    private final PlanWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * The client's plan, with exercise names in the language of {@code Accept-Language}. {@code 404} with
     * {@code client.not-found} for an unknown client and another trainer's alike, and with {@code plan.not-found} for an
     * own client with no plan.
     */
    @GetMapping
    PlanResponse getPlan(@PathVariable UUID id, Locale locale, Authentication authentication) {
        return mapper.toResponse(plans.get(signedIn(authentication), new AccountId(id)), locale);
    }

    /**
     * Saves the plan as a whole; the first save creates a draft, and a published plan stays published with the same
     * first week. {@code 409} with {@code plan.changed} when the version is outdated, and with
     * {@code plan.quiz-not-submitted} on a first save before the client submitted the quiz.
     */
    @PutMapping
    PlanResponse savePlan(
            @PathVariable UUID id,
            @Valid @RequestBody SavePlanRequest request,
            Locale locale,
            Authentication authentication) {
        return mapper.toResponse(
                plans.save(mapper.toCommand(signedIn(authentication), new AccountId(id), request)), locale);
    }

    /**
     * Publishes the saved draft; week 1 becomes the current week in the client's time zone. {@code 409} with
     * {@code plan.already-published} or {@code plan.changed}, {@code 400} when the draft is not complete.
     */
    @PostMapping("/publication")
    PlanResponse publishPlan(
            @PathVariable UUID id,
            @Valid @RequestBody PublishPlanRequest request,
            Locale locale,
            Authentication authentication) {
        return mapper.toResponse(
                plans.publish(signedIn(authentication), new AccountId(id), request.version()), locale);
    }
}
