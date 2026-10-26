package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.AccountId;

public interface AccountSessions {

    /** Ends every sign-in of the account, on every device. */
    void endAll(AccountId id);
}
