package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.AccountId;

/** A signed-in person deletes their own account and everything that exists only because of it. */
public interface DeleteAccountUseCase {

    DeletionCheck check(AccountId id);

    /**
     * Deletes the account after checking, in this order: the sign-in lock of its email ({@code account.sign-in-locked}),
     * the password ({@code account.password-incorrect}, counted as a failed sign-in), and for a trainer that no client
     * is left ({@code account.has-clients}). Every sign-in of the account ends afterwards.
     */
    void delete(AccountId id, String password);
}
