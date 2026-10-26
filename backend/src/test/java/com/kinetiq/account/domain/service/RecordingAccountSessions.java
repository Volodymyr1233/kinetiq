package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.output.AccountSessions;
import java.util.ArrayList;
import java.util.List;

class RecordingAccountSessions implements AccountSessions {

    final List<AccountId> ended = new ArrayList<>();

    @Override
    public void endAll(AccountId id) {
        ended.add(id);
    }
}
