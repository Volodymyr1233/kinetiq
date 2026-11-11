package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.output.PlanChangeApprovals;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Answers the entries it was given, and remembers what it was asked. */
class FakePlanChangeApprovals implements PlanChangeApprovals {

    final List<Entry> entries = new ArrayList<>();
    AccountId askedBy;
    Instant askedSince;

    @Override
    public List<Entry> forTrainer(AccountId trainer, Instant endedSince) {
        askedBy = trainer;
        askedSince = endedSince;
        return entries;
    }
}
