package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import java.util.List;

public interface DetectPlanChangesUseCase {

    /** The clients whose check week has begun and has not been checked yet. */
    List<AccountId> due();

    /** Checks one client for the current check week, once; later calls in the same week do nothing. */
    void check(AccountId client);
}
