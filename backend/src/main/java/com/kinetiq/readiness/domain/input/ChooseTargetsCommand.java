package com.kinetiq.readiness.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.util.Objects;

public record ChooseTargetsCommand(AccountId client, TargetChoice choice) {

    public ChooseTargetsCommand {
        Objects.requireNonNull(client);
        Objects.requireNonNull(choice);
    }
}
