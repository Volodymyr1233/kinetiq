package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.output.PlanDraftingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class InMemoryPlanDraftingRepository implements PlanDraftingRepository {

    final Map<AccountId, Instant> rows = new LinkedHashMap<>();

    @Override
    public void start(AccountId client, Instant at) {
        rows.put(client, at);
    }

    @Override
    public boolean remove(AccountId client) {
        return rows.remove(client) != null;
    }

    @Override
    public List<AccountId> startedBefore(Instant instant) {
        return rows.entrySet().stream()
                .filter(row -> row.getValue().isBefore(instant))
                .map(Map.Entry::getKey)
                .toList();
    }
}
