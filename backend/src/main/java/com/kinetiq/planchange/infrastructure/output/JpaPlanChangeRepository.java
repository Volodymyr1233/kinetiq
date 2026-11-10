package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.model.PlanChangeState;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaPlanChangeRepository extends JpaRepository<PlanChangeEntity, UUID> {

    boolean existsByClientIdAndStateIn(UUID clientId, Collection<PlanChangeState> states);

    Optional<PlanChangeEntity> findByClientIdAndStateIn(UUID clientId, Collection<PlanChangeState> states);

    @Query("select max(change.endedAt) from PlanChangeEntity change where change.clientId = :clientId")
    Optional<Instant> lastEndedAt(UUID clientId);
}
