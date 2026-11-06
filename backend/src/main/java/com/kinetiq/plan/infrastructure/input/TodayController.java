package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/today/** needs the role CLIENT; "today" is the signed-in client's own, so no id is taken from the request
@RestController
@RequestMapping("/api/v1/today")
@RequiredArgsConstructor
class TodayController {

    private final ViewOwnPlanUseCase plans;
    private final PlanWebMapper mapper;

    /**
     * The signed-in client's session today in their time zone, with exercise names in the language of
     * {@code Accept-Language}, or {@code REST} for a day without one and {@code FINISHED} once the plan has ended.
     * {@code 404} with {@code plan.not-found} for no plan and for a draft alike.
     */
    @GetMapping
    TodayResponse getToday(Locale locale, Authentication authentication) {
        // sign-in stores the account id as the principal's name
        AccountId client = new AccountId(UUID.fromString(authentication.getName()));
        return mapper.toResponse(plans.today(client), locale);
    }
}
