package com.kinetiq.exercise.infrastructure.output;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaExerciseRepository extends JpaRepository<ExerciseEntity, UUID> {

    List<ExerciseEntity> findByOwnerIdIsNullOrOwnerId(UUID ownerId);
}
