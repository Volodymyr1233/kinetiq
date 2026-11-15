package com.kinetiq.plan.infrastructure.output;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaPlanRevisionRepository extends JpaRepository<PlanRevisionEntity, UUID> {

    @EntityGraph(attributePaths = "sessions")
    List<PlanRevisionEntity> findByPlanIdOrderByValidUntilAsc(UUID planId);
}
