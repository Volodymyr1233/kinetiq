package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Holds one open flag and one end time for every client, and the plan changes handed over. */
class FakePlanChangeRepository implements PlanChangeRepository {

    boolean open;
    Instant endedAt;
    final List<DuePlanChange> opened = new ArrayList<>();

    @Override
    public boolean hasOpen(AccountId client) {
        return open;
    }

    @Override
    public Optional<Instant> lastEndedAt(AccountId client) {
        return Optional.ofNullable(endedAt);
    }

    @Override
    public void open(DuePlanChange due) {
        opened.add(due);
    }
}
