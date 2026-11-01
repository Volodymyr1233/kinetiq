package com.kinetiq.exercise.infrastructure.output;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaExerciseRepository extends JpaRepository<ExerciseEntity, UUID> {

    List<ExerciseEntity> findByOwnerIdIsNullOrOwnerId(UUID ownerId);

    // plans belong to another feature, so they are read by SQL and not by class; a plan using an exercise twice counts once
    @Query(
            value = """
                    select e.exercise_id as exerciseId, count(distinct s.plan_id) as plans
                    from plan_session_entries e
                    join plan_sessions s on s.id = e.session_id
                    join plans p on p.id = s.plan_id
                    join accounts client on client.id = p.client_id
                    where client.trainer_id = :trainer
                    group by e.exercise_id
                    """,
            nativeQuery = true)
    List<PlansUsing> countPlansUsing(UUID trainer);

    interface PlansUsing {

        UUID getExerciseId();

        int getPlans();
    }
}
