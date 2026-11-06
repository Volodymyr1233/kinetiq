package com.kinetiq.quiz.infrastructure.output;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaQuizSubmissionRepository extends JpaRepository<QuizSubmissionEntity, UUID> {

    boolean existsByClientId(UUID clientId);

    Optional<QuizSubmissionEntity> findByClientId(UUID clientId);

    // plans and draftings belong to a feature that depends on this one, so they are read by SQL and not by class
    @Query(
            value = "select exists (select 1 from plans where client_id = :clientId and first_week is not null)",
            nativeQuery = true)
    boolean hasPublishedPlan(UUID clientId);

    @Query(value = "select exists (select 1 from plan_draftings where client_id = :clientId)", nativeQuery = true)
    boolean isPlanDrafting(UUID clientId);
}
