package com.kinetiq.exercise.infrastructure.output;

import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class ExerciseRepositoryImpl implements ExerciseRepository {

    private final JpaExerciseRepository jpa;
    private final ExerciseMapper mapper;

    @Override
    public List<Exercise> findStarter() {
        return jpa.findByOwnerIdIsNull().stream().map(mapper::toDomain).toList();
    }
}
