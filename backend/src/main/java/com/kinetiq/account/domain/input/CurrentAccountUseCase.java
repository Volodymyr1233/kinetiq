package com.kinetiq.account.domain.input;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import java.time.ZoneId;

public interface CurrentAccountUseCase {

    Account find(AccountId id);

    /** Stores the browser's time zone; the same zone changes nothing. */
    void changeTimeZone(AccountId id, ZoneId zone);

    /** Changes the values the command sends and answers the account as it is now. */
    Account update(AccountId id, UpdateAccountCommand command);
}
