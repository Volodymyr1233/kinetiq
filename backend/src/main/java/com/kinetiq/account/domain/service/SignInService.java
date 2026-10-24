package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.CredentialsInvalidException;
import com.kinetiq.account.domain.exception.EmailNotConfirmedException;
import com.kinetiq.account.domain.input.SignInCommand;
import com.kinetiq.account.domain.input.SignInUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.common.domain.model.Email;
import java.util.Optional;

public class SignInService implements SignInUseCase {

    private final AccountRepository accounts;
    private final PasswordHasher hasher;

    // compared against for an unknown email, so it costs as long as a known one
    private final String dummyHash;

    public SignInService(AccountRepository accounts, PasswordHasher hasher) {
        this.accounts = accounts;
        this.hasher = hasher;
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
        Optional<Account> account = accounts.findByEmail(email);
        boolean matches = hasher.matches(password, account.map(Account::getPasswordHash).orElse(dummyHash));
        if (account.isEmpty() || !matches) {
            throw new CredentialsInvalidException();
        }
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
