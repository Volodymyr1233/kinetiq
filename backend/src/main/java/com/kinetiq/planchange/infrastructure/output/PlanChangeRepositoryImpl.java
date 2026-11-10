package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
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

    @Override
    public Optional<PlanChange> findOpen(AccountId client) {
        return jpa.findByClientIdAndStateIn(client.value(), OPEN).map(mapper::toDomain);
    }
}
