package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PlanChangeRepositoryImpl implements PlanChangeRepository {

    private static final List<PlanChangeState> OPEN = Arrays.stream(PlanChangeState.values())
            .filter(PlanChangeState::isOpen)
            .toList();

    private final JpaPlanChangeRepository jpa;
    private final PlanChangeMapper mapper;
    private final EntityManager entityManager;

    @Override
    public boolean hasOpen(AccountId client) {
        return jpa.existsByClientIdAndStateIn(client.value(), OPEN);
    }

    @Override
    public Optional<Instant> lastEndedAt(AccountId client) {
        return jpa.lastEndedAt(client.value());
    }

    // flushed at once, so a second open plan change of the client fails inside this call
    @Override
    public PlanChange save(PlanChange planChange) {
        jpa.saveAndFlush(mapper.toEntity(planChange));
        return planChange;
    }

    @Override
    public Optional<PlanChange> find(PlanChangeId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    // refreshed under the lock: a copy loaded earlier in this transaction would hide what the holder of the lock stored
    @Override
    public Optional<PlanChange> findForUpdate(PlanChangeId id) {
        return jpa.findById(id.value()).map(entity -> {
            entityManager.refresh(entity, LockModeType.PESSIMISTIC_WRITE);
            return mapper.toDomain(entity);
        });
    }

    // only the decision columns change, so the content rows of the plan change are left alone
    @Override
    public PlanChange update(PlanChange planChange) {
        PlanChangeEntity entity = jpa.findById(planChange.getId().value()).orElseThrow();
        mapper.applyDecision(planChange, entity);
        jpa.saveAndFlush(entity);
        return planChange;
    }

    @Override
    public Optional<PlanChange> findOpen(AccountId client) {
        return jpa.findByClientIdAndStateIn(client.value(), OPEN).map(mapper::toDomain);
    }
}
