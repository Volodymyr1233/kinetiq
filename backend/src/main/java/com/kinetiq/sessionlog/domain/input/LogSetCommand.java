package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.math.BigDecimal;

/** A set to log or to change; {@code loadKg} is {@code null} for no added weight. */
public record LogSetCommand(
        AccountId client,
        SessionLogId session,
        SetLogId setLogId,
        int entryPosition,
        int setNumber,
        BigDecimal loadKg,
        int amount,
        int rpe) {}
