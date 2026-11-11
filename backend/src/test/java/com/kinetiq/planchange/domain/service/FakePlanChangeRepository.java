package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** Holds the saved plan changes, plus an open flag and one end time that stand for any client's earlier ones. */
class FakePlanChangeRepository implements PlanChangeRepository {

    boolean open;
    Instant endedAt;
    final List<PlanChange> saved = new ArrayList<>();

    @Override
    public boolean hasOpen(AccountId client) {
        return open || findOpen(client).isPresent();
    }

    @Override
    public Optional<Instant> lastEndedAt(AccountId client) {
        return Optional.ofNullable(endedAt);
    }

    @Override
    public PlanChange save(PlanChange planChange) {
        if (planChange.isOpen() && findOpen(planChange.getClient()).isPresent()) {
            throw new IllegalStateException("a second open plan change for the client");
        }
        saved.add(planChange);
        return planChange;
    }

    @Override
    public Optional<PlanChange> find(PlanChangeId id) {
        return saved.stream().filter(change -> change.getId().equals(id)).findFirst();
    }

    @Override
    public Optional<PlanChange> findForUpdate(PlanChangeId id) {
        return find(id);
    }

    @Override
    public PlanChange update(PlanChange planChange) {
        saved.replaceAll(change -> change.getId().equals(planChange.getId()) ? planChange : change);
        return planChange;
    }

    @Override
    public List<PlanChange> findAll(Collection<PlanChangeId> ids) {
        return saved.stream().filter(change -> ids.contains(change.getId())).toList();
    }

    @Override
    public List<PlanChange> findEnded(AccountId client, int limit) {
        return saved.stream()
                .filter(change -> change.getClient().equals(client) && !change.isOpen())
                .sorted(Comparator.comparing((PlanChange change) -> change.getEndedAt().orElse(change.getCreatedAt())).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public Optional<PlanChange> findOpen(AccountId client) {
        return saved.stream()
                .filter(change -> change.getClient().equals(client) && change.isOpen())
                .findFirst();
    }
}
