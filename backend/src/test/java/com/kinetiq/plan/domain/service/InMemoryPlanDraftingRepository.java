package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftingKind;
import com.kinetiq.plan.domain.output.PlanDraftingRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

class InMemoryPlanDraftingRepository implements PlanDraftingRepository {

    record Row(Instant startedAt, DraftingKind kind) {}

    final Map<AccountId, Row> rows = new LinkedHashMap<>();

    @Override
    public void start(AccountId client, Instant at, DraftingKind kind) {
        rows.put(client, new Row(at, kind));
    }

    @Override
    public Optional<DraftingKind> remove(AccountId client) {
        return Optional.ofNullable(rows.remove(client)).map(Row::kind);
    }

    @Override
    public boolean exists(AccountId client) {
        return rows.containsKey(client);
    }

    @Override
    public List<AccountId> startedBefore(Instant instant) {
        return rows.entrySet().stream()
                .filter(row -> row.getValue().startedAt().isBefore(instant))
                .map(Map.Entry::getKey)
                .toList();
    }
}
