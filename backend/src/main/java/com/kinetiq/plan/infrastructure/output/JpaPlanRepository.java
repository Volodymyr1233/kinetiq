package com.kinetiq.plan.infrastructure.output;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

interface JpaPlanRepository extends JpaRepository<PlanEntity, UUID> {

    Optional<PlanEntity> findByClientId(UUID clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from PlanEntity plan where plan.clientId = :clientId")
    Optional<PlanEntity> findByClientIdForUpdate(UUID clientId);
}
