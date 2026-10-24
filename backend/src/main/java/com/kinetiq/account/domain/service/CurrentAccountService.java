package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.AccountNotFoundException;
import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.output.AccountRepository;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class CurrentAccountService implements CurrentAccountUseCase {

    private final AccountRepository accounts;

    @Override
    public Account find(AccountId id) {
        return accounts.findById(id).orElseThrow(AccountNotFoundException::new);
    }

    @Override
    public void changeTimeZone(AccountId id, ZoneId zone) {
        Account account = find(id);
        if (account.getTimeZone().equals(zone)) {
            return;
        }
        account.changeTimeZone(zone);
        accounts.save(account);
    }
}
