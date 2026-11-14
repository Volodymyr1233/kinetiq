package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;

/** Accounts where every id is a client of one trainer, for tests that only need the trainer of a client. */
public final class TrainerOfEveryClient implements CurrentAccountUseCase {

    private final AccountId trainer;

    public TrainerOfEveryClient(AccountId trainer) {
        this.trainer = trainer;
    }

    @Override
    public Account find(AccountId id) {
        return Account.restore(
                id,
                "Client",
                new Email("client@example.com"),
                "hash",
                Role.CLIENT,
                trainer,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null,
                null);
    }

    @Override
    public void changeTimeZone(AccountId id, ZoneId zone) {}

    @Override
    public Account update(AccountId id, UpdateAccountCommand command) {
        return find(id);
    }
}
