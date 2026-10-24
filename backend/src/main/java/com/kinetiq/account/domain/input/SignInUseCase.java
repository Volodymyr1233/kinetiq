package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.Account;

public interface SignInUseCase {

    /**
     * Returns the account whose email and password match. An unknown email and a wrong password fail the same way and
     * take about as long; a trainer whose email is not confirmed is refused only after the right password.
     */
    Account signIn(SignInCommand command);
}
