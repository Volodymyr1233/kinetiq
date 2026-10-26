package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.PasswordResetLinkInvalidException;
import com.kinetiq.account.domain.input.ResetPasswordUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.AccountSessions;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PasswordResetService implements ResetPasswordUseCase {

    private static final String RESET_PASSWORD = "reset-password";

    private final AccountRepository accounts;
    private final PasswordHasher hasher;
    private final AccountSessions sessions;
    private final MailSender mailSender;
    private final Clock clock;
    private final URI appUrl;

    @Override
    public void requestReset(Email email, Locale locale) {
        Instant now = clock.instant();
        accounts.findByEmail(email)
                .filter(account -> account.isEmailConfirmed() && account.canReceiveMail(now))
                .ifPresent(account -> {
                    SecretToken token = SecretToken.generate();
                    account.issuePasswordReset(token, now);
                    accounts.save(account);
                    // a fragment never reaches a server, so the token stays out of access logs and Referer headers
                    String link = appUrl + "/reset-password#token=" + token.value();
                    mailSender.send(new Mail(account.getEmail(), RESET_PASSWORD, locale, Map.of("link", link)));
                });
    }

    @Override
    public void setPassword(String token, RawPassword password) {
        Account account = accounts.findByPasswordResetTokenHash(new SecretToken(token).hash())
                .orElseThrow(PasswordResetLinkInvalidException::new);
        account.resetPassword(hasher.hash(password), clock.instant());
        accounts.save(account);
        sessions.endAll(account.getId());
    }
}
