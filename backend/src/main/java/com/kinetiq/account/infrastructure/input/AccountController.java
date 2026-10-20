package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.SignUpTrainerCommand;
import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import jakarta.validation.Valid;
import java.time.ZoneId;
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

    /** Answers {@code 202} whether or not the email already had an account. */
    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void signUp(@Valid @RequestBody SignUpRequest request, Locale locale) {
        signUp.signUpTrainer(new SignUpTrainerCommand(
                request.name(),
                new Email(request.email()),
                new RawPassword(request.password()),
                timeZone(request.timeZone()),
                locale));
    }

    @PostMapping("/email-confirmations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void confirmEmail(@Valid @RequestBody ConfirmEmailRequest request) {
        signUp.confirmEmail(request.token());
    }

    /** Answers {@code 202} whatever the email. */
    @PostMapping("/confirmation-links")
    @ResponseStatus(HttpStatus.ACCEPTED)
    void sendConfirmationLink(@Valid @RequestBody ConfirmationLinkRequest request, Locale locale) {
        signUp.sendConfirmationLink(new Email(request.email()), locale);
    }

    // region ids only: offsets such as +02:00 would not follow daylight saving time
    private static ZoneId timeZone(String id) {
        if (!ZoneId.getAvailableZoneIds().contains(id)) {
            throw new InvalidValueException("unknown time zone");
        }
        return ZoneId.of(id);
    }
}
