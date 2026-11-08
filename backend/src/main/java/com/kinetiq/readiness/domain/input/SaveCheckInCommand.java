package com.kinetiq.readiness.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.Answers;
import java.util.Objects;

public record SaveCheckInCommand(AccountId client, Answers answers) {

    public SaveCheckInCommand {
        Objects.requireNonNull(client);
        Objects.requireNonNull(answers);
    }
}
