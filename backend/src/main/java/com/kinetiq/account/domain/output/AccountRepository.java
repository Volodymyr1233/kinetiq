package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    void save(Account account);

    Optional<Account> findById(AccountId id);

    Optional<Account> findByEmail(Email email);

    boolean existsByEmail(Email email);

    Optional<Account> findByConfirmationTokenHash(String tokenHash);

    Optional<Account> findByPasswordResetTokenHash(String tokenHash);

    /** The trainer's clients, ordered by name. */
    List<Account> findClientsOf(AccountId trainerId);

    /**
     * Deletes the trainer accounts whose email is still unconfirmed and whose confirmation link expired before
     * {@code linkExpiredBefore}, and answers how many there were. Client accounts are never deleted here.
     */
    int deleteUnconfirmedTrainers(Instant linkExpiredBefore);
}
