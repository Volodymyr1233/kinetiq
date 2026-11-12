package com.kinetiq.overview.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.overview.domain.input.ViewOverviewUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// /api/v1/clients/** needs the role TRAINER; the use case reads only the trainer's own clients
@RestController
@RequiredArgsConstructor
class OverviewController {

    private final ViewOverviewUseCase overview;
    private final OverviewWebMapper mapper;

    /** The overview of the signed-in trainer's own clients: the tiles, the alerts and a row per client. */
    @GetMapping("/api/v1/clients/overview")
    OverviewResponse trainerOverview(Authentication authentication) {
        // sign-in stores the account id as the principal's name
        return mapper.toResponse(overview.overview(new AccountId(UUID.fromString(authentication.getName()))));
    }
}
