package com.kinetiq.exercise.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.exception.ExerciseNameTakenException;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ExerciseRepositoryImpl implements ExerciseRepository {

    private static final String OWNER_NAME_INDEX = "ux_exercises_owner_name";

    private final JpaExerciseRepository jpa;
    private final ExerciseMapper mapper;

    @Override
    public List<Exercise> findVisibleTo(AccountId trainer) {
        return jpa.findByOwnerIdIsNullOrOwnerId(trainer.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Exercise> findById(ExerciseId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    // updates the managed entity, so its version and timestamps survive; flushed at once, so a create that lost a race
    // on the name index fails here, where it can be told apart from other failures
    @Override
    public void save(Exercise exercise) {
        ExerciseEntity entity = jpa.findById(exercise.getId().value()).orElseGet(ExerciseEntity::new);
        mapper.update(exercise, entity);
        try {
            jpa.saveAndFlush(entity);
        } catch (DataIntegrityViolationException exception) {
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && OWNER_NAME_INDEX.equals(violation.getConstraintName())) {
                throw new ExerciseNameTakenException();
            }
            throw exception;
        }
    }
}
