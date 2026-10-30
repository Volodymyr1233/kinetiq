package com.kinetiq.quiz.infrastructure.output;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaQuizSubmissionRepository extends JpaRepository<QuizSubmissionEntity, UUID> {

    boolean existsByClientId(UUID clientId);
}
