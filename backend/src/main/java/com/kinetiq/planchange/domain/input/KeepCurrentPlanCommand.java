package com.kinetiq.planchange.domain.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.DeclineReason;
import com.kinetiq.planchange.domain.model.PlanChangeId;

/** {@code person} keeps the current plan instead of accepting a plan change; {@code note} may be {@code null}. */
public record KeepCurrentPlanCommand(AccountId person, PlanChangeId id, DeclineReason reason, String note) {}
