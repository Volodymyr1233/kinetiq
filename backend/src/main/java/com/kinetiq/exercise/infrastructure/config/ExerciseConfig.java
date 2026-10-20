package com.kinetiq.exercise.infrastructure.config;

import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.output.ExerciseRepository;
import com.kinetiq.exercise.domain.service.ExerciseListingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ExerciseConfig {

    @Bean
    ListExercisesUseCase listExercisesUseCase(ExerciseRepository exercises) {
        return new ExerciseListingService(exercises);
    }
}
