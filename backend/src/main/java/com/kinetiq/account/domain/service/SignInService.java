package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.CredentialsInvalidException;
import com.kinetiq.account.domain.exception.EmailNotConfirmedException;
import com.kinetiq.account.domain.exception.SignInLockedException;
import com.kinetiq.account.domain.input.SignInCommand;
import com.kinetiq.account.domain.input.SignInUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.FailedSignIns;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.FailedSignInsRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

public class SignInService implements SignInUseCase {

    private final AccountRepository accounts;
    private final PasswordHasher hasher;
    private final FailedSignInsRepository failedSignIns;
    private final Clock clock;

    // compared against for an unknown email, so it costs as long as a known one
    private final String dummyHash;

    public SignInService(
            AccountRepository accounts, PasswordHasher hasher, FailedSignInsRepository failedSignIns, Clock clock) {
        this.accounts = accounts;
        this.hasher = hasher;
        this.failedSignIns = failedSignIns;
        this.clock = clock;
        this.dummyHash = hasher.hash(new RawPassword("Not-a-password-1"));
    }

    @Override
    public Account signIn(SignInCommand command) {
        String password = command.password();
        Email email = parse(command.email()).orElseThrow(CredentialsInvalidException::new);
        // no account can have such a password, so there is nothing to hash
        if (password.length() > RawPassword.MAX_LENGTH) {
            throw new CredentialsInvalidException();
        }
        Instant now = clock.instant();
        Optional<FailedSignIns> failed = failedSignIns.find(email);
        Optional<Duration> timeLeft = failed.flatMap(entry -> entry.timeLeft(now));
        if (timeLeft.isPresent()) {
            throw new SignInLockedException(timeLeft.get());
        }
        Optional<Account> account = accounts.findByEmail(email);
        boolean matches = hasher.matches(password, account.map(Account::getPasswordHash).orElse(dummyHash));
        if (account.isEmpty() || !matches) {
            FailedSignIns entry = failed.orElseGet(() -> new FailedSignIns(email));
            entry.recordFailure(now);
            failedSignIns.save(entry);
            throw new CredentialsInvalidException();
        }
        failedSignIns.delete(email);
        Account found = account.get();
        if (found.getRole() == Role.TRAINER && !found.isEmailConfirmed()) {
            throw new EmailNotConfirmedException();
        }
        return found;
    }

    private static Optional<Email> parse(String email) {
        try {
            return Optional.of(new Email(email));
        } catch (InvalidValueException exception) {
            return Optional.empty();
        }
    }
}
