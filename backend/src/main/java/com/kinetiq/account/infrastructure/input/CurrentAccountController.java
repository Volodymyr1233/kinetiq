package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.AccountId;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts/me")
@RequiredArgsConstructor
class CurrentAccountController {

    private final CurrentAccountUseCase currentAccount;
    private final AccountWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /** Answers who is signed in. */
    @GetMapping
    CurrentAccountResponse find(Authentication authentication) {
        return mapper.toResponse(currentAccount.find(signedIn(authentication)));
    }

    /** Stores the browser's time zone, so weeks and days follow the person. */
    @PutMapping("/time-zone")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void changeTimeZone(@Valid @RequestBody TimeZoneRequest request, Authentication authentication) {
        currentAccount.changeTimeZone(signedIn(authentication), TimeZones.parse(request.timeZone()));
    }
}
