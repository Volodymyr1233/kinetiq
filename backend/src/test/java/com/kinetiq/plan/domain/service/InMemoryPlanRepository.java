package com.kinetiq.plan.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.output.PlanRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

class InMemoryPlanRepository implements PlanRepository {

    final Map<AccountId, Plan> byClient = new LinkedHashMap<>();

    @Override
    public Optional<Plan> findByClient(AccountId client) {
        return Optional.ofNullable(byClient.get(client));
    }

    @Override
    public Optional<Plan> findByClientForUpdate(AccountId client) {
        return findByClient(client);
    }

    // honours the unique constraint on the client and raises the version, as the adapter does
    @Override
    public Plan save(Plan plan) {
        Plan stored = byClient.get(plan.getClient());
        if (stored != null && !stored.getId().equals(plan.getId())) {
            throw new PlanChangedException();
        }
        Plan saved = new Plan(
                plan.getId(),
                plan.getClient(),
                plan.getGoal(),
                plan.getWeeks(),
                plan.getFirstWeek().orElse(null),
                plan.getSessions(),
                stored == null ? 0 : stored.getVersion() + 1);
        byClient.put(plan.getClient(), saved);
        return saved;
    }
}
