package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.common.domain.model.Email;
import java.util.Optional;

public interface AccountRepository {

    void save(Account account);

    Optional<Account> findByEmail(Email email);

    Optional<Account> findByConfirmationTokenHash(String tokenHash);
}
