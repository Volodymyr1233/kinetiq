package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.exception.InviteLimitReachedException;
import com.kinetiq.account.domain.input.CreateInviteCommand;
import com.kinetiq.account.domain.input.ManageInvitesUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.common.domain.model.Email;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invites")
@RequiredArgsConstructor
class InviteController {

    private final ManageInvitesUseCase invites;
    private final InviteWebMapper mapper;
    private final RequestLimiter limiter;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * Invites the email in the language of {@code Accept-Language} and returns the link to copy. Refused with
     * {@code 409} when the email has an account, and {@code 429} after 20 requests in an hour, refused ones included.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CreatedInviteResponse create(
            @Valid @RequestBody CreateInviteRequest request, Locale locale, Authentication authentication) {
        AccountId trainerId = signedIn(authentication);
        if (!limiter.tryAcquire(RequestLimiter.Kind.INVITE, trainerId.value().toString())) {
            throw new InviteLimitReachedException();
        }
        return mapper.toResponse(invites.create(new CreateInviteCommand(trainerId, new Email(request.email()), locale)));
    }

    /** Lists the trainer's open invites, newest first. */
    @GetMapping
    InviteListResponse listOpen(Authentication authentication) {
        return mapper.toListResponse(invites.listOpen(signedIn(authentication)));
    }

    /** Issues a new link to copy; the one copied before stops working. {@code 404} unless the invite is own and open. */
    @PostMapping("/{id}/links")
    @ResponseStatus(HttpStatus.CREATED)
    InviteLinkResponse replaceLink(@PathVariable UUID id, Authentication authentication) {
        return new InviteLinkResponse(invites.replaceLink(signedIn(authentication), new InviteId(id)));
    }

    /** Revokes the invite, so neither link works. {@code 404} unless the invite is own and open. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable UUID id, Authentication authentication) {
        invites.revoke(signedIn(authentication), new InviteId(id));
    }
}
