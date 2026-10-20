package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class ExerciseController {

    private final ListExercisesUseCase listExercises;
    private final ExerciseWebMapper mapper;

    /** Lists the library filtered by category and name, in the language of {@code Accept-Language}. */
    @GetMapping("/exercises")
    ExerciseListResponse listExercises(@Valid ListExercisesRequest request, Locale locale) {
        return mapper.toResponse(
                listExercises.list(new ListExercisesQuery(request.category(), request.name(), locale)), locale);
    }
}
