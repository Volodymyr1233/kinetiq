package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.input.ListDraftedPlansUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/approvals/** needs the role TRAINER; the query reads only this trainer's clients
@RestController
@RequiredArgsConstructor
class DraftedPlansController {

    private final ListDraftedPlansUseCase drafts;

    /**
     * The signed-in trainer's clients whose plan is a draft, oldest first. A client whose first plan is still being
     * drafted is absent; a draft being built again stays.
     */
    @GetMapping("/api/v1/approvals/drafted-plans")
    DraftedPlansResponse draftedPlans(Authentication authentication) {
        // sign-in stores the account id as the principal's name
        AccountId trainer = new AccountId(UUID.fromString(authentication.getName()));
        return DraftedPlansResponse.of(drafts.draftedPlans(trainer));
    }
}
