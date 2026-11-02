package com.kinetiq.plan.infrastructure.output;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaPlanRepository extends JpaRepository<PlanEntity, UUID> {

    Optional<PlanEntity> findByClientId(UUID clientId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from PlanEntity plan where plan.clientId = :clientId")
    Optional<PlanEntity> findByClientIdForUpdate(UUID clientId);

    boolean existsByClientIdAndClientSeenAtIsNotNull(UUID clientId);

    // a bulk update, so it neither raises the version nor loads the sessions; it matches a published, unseen plan once
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(
            "update PlanEntity plan set plan.clientSeenAt = :at"
                    + " where plan.clientId = :clientId and plan.firstWeek is not null and plan.clientSeenAt is null")
    void markSeen(UUID clientId, Instant at);
}
