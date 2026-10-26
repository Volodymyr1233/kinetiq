package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.ResetPasswordUseCase;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.common.domain.model.Email;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class PasswordResetController {

    private final ResetPasswordUseCase resetPassword;
    private final RequestLimiter limiter;

    /** Answers {@code 202} whatever the email, and also when the address asked too often, which then sends nothing. */
    @PostMapping("/password-reset-links")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void requestReset(@Valid @RequestBody PasswordResetLinkRequest request, Locale locale, HttpServletRequest http) {
        if (!limiter.tryAcquire(RequestLimiter.Kind.PASSWORD_RESET, http.getRemoteAddr())) {
            return;
        }
        resetPassword.requestReset(new Email(request.email()), locale);
    }

    @PostMapping("/password-resets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void setPassword(@Valid @RequestBody SetPasswordRequest request) {
        resetPassword.setPassword(request.token(), new RawPassword(request.password()));
    }
}
