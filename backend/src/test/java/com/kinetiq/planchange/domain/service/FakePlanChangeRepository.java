package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Holds the saved plan changes, plus an open flag and one end time that stand for any client's earlier ones. */
class FakePlanChangeRepository implements PlanChangeRepository {

    boolean open;
    Instant endedAt;
    final List<PlanChange> saved = new ArrayList<>();
    final Map<AccountId, ZoneId> zones = new HashMap<>();

    @Override
    public boolean hasUnsettled(AccountId client) {
        return open || saved.stream().anyMatch(change -> change.getClient().equals(client) && change.isUnsettled());
    }

    // like the adapter, the date is the one at the instant in the client's zone, which is UTC unless the test sets one
    @Override
    public List<PlanChangeId> findDue(Instant now) {
        return saved.stream()
                .filter(PlanChange::isUnsettled)
                .filter(change -> !change.getEffectiveWeek()
                        .isAfter(now.atZone(zones.getOrDefault(change.getClient(), ZoneOffset.UTC)).toLocalDate()))
                .map(PlanChange::getId)
                .toList();
    }

    @Override
    public Optional<Instant> lastEndedAt(AccountId client) {
        return Optional.ofNullable(endedAt);
    }

    @Override
    public PlanChange save(PlanChange planChange) {
        if (planChange.isUnsettled() && saved.stream().anyMatch(change ->
                change.getClient().equals(planChange.getClient()) && change.isUnsettled())) {
            throw new IllegalStateException("a second unsettled plan change for the client");
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
    public Optional<PlanChange> findUpcoming(AccountId client) {
        return saved.stream()
                .filter(change -> change.getClient().equals(client) && change.isUpcoming())
                .findFirst();
    }

    @Override
    public Optional<PlanChange> findLatestInEffect(AccountId client) {
        return saved.stream()
                .filter(change -> change.getClient().equals(client) && change.isInEffect())
                .max(Comparator.comparing(change -> change.getTookEffectAt().orElseThrow()));
    }

    @Override
    public Optional<PlanChange> findOpen(AccountId client) {
        return saved.stream()
                .filter(change -> change.getClient().equals(client) && change.isOpen())
                .findFirst();
    }
}
