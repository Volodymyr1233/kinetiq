package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.EmailConfirmation;
import com.kinetiq.account.domain.model.PasswordReset;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.common.domain.model.Email;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryAccountRepository implements AccountRepository {

    final Map<Email, Account> byEmail = new HashMap<>();
    int saves;

    Account add(Account account) {
        byEmail.put(account.getEmail(), account);
        return account;
    }

    @Override
    public void save(Account account) {
        saves++;
        add(account);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return byEmail.values().stream()
                .filter(account -> account.getId().equals(id))
                .findFirst();
    }

    @Override
    public Optional<Account> findByEmail(Email email) {
        return Optional.ofNullable(byEmail.get(email));
    }

    @Override
    public boolean existsByEmail(Email email) {
        return byEmail.containsKey(email);
    }

    @Override
    public Optional<Account> findByConfirmationTokenHash(String tokenHash) {
        return byEmail.values().stream()
                .filter(account -> account.getConfirmation()
                        .map(EmailConfirmation::tokenHash)
                        .filter(tokenHash::equals)
                        .isPresent())
                .findFirst();
    }

    @Override
    public Optional<Account> findByPasswordResetTokenHash(String tokenHash) {
        return byEmail.values().stream()
                .filter(account -> account.getPasswordReset()
                        .map(PasswordReset::tokenHash)
                        .filter(tokenHash::equals)
                        .isPresent())
                .findFirst();
    }
}
