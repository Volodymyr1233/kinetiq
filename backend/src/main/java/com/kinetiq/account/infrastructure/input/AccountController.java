package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.SignUpTrainerCommand;
import com.kinetiq.account.domain.input.SignUpUseCase;
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
class AccountController {

    private final SignUpUseCase signUp;
    private final RequestLimiter limiter;

    /**
     * Answers {@code 202} whether or not the email already had an account, and also when the address made too many
     * sign-ups, which then creates nothing.
     */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void signUp(@Valid @RequestBody SignUpRequest request, Locale locale, HttpServletRequest http) {
        if (!limiter.tryAcquire(RequestLimiter.Kind.SIGN_UP, http.getRemoteAddr())) {
            return;
        }
        signUp.signUpTrainer(new SignUpTrainerCommand(
                request.name(),
                new Email(request.email()),
                new RawPassword(request.password()),
                TimeZones.parse(request.timeZone()),
                locale));
    }

    @PostMapping("/email-confirmations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        signUp.confirmEmail(request.token());
    }

    /** Answers {@code 202} whatever the email, and also when the address asked too often, which then sends nothing. */
    @PostMapping("/confirmation-links")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void sendConfirmationLink(
            @Valid @RequestBody ConfirmationLinkRequest request, Locale locale, HttpServletRequest http) {
        if (!limiter.tryAcquire(RequestLimiter.Kind.CONFIRMATION_LINK, http.getRemoteAddr())) {
            return;
        }
        signUp.sendConfirmationLink(new Email(request.email()), locale);
    }
}
