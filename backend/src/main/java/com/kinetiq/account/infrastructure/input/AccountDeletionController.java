package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.DeleteAccountUseCase;
import com.kinetiq.account.domain.input.DeletionCheck;
import com.kinetiq.account.domain.model.AccountId;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in person's own account deletion; no address names an account, so nobody can delete another. */
@RestController
@RequestMapping("/api/v1/accounts/me/deletion")
@RequiredArgsConstructor
class AccountDeletionController {

    private final DeleteAccountUseCase deletion;
    private final LogoutHandler logoutHandler;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /** Answers what the confirmation needs: a trainer's number of clients, a client's trainer's name. */
    @GetMapping
    AccountDeletionResponse check(Authentication authentication) {
        DeletionCheck check = deletion.check(signedIn(authentication));
        return new AccountDeletionResponse(check.clients(), check.trainerName());
    }

    /**
     * Deletes the account and ends every sign-in of it, this browser's included. Refusals, which delete nothing:
     * {@code 429 account.sign-in-locked}, {@code 400 account.password-incorrect} and {@code 409 account.has-clients}.
     */
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(
            @Valid @RequestBody DeleteAccountRequest body,
            Authentication authentication,
            HttpServletRequest request,
            HttpServletResponse response) {
        deletion.delete(signedIn(authentication), body.password());
        logoutHandler.logout(request, response, authentication);
    }
}
