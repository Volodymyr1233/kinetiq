package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.ListExercisesQuery;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.exercise.domain.input.ManageExercisesUseCase;
import com.kinetiq.exercise.domain.model.ExerciseId;
import jakarta.validation.Valid;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// no delete mapping: exercises are never deleted, and Spring answers a delete request with 405
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class ExerciseController {

    private final ListExercisesUseCase listExercises;
    private final ManageExercisesUseCase manageExercises;
    private final ExerciseWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /**
     * Lists the trainer's library, the starter exercises and their own, filtered by category and name, in the language
     * of {@code Accept-Language}.
     */
    @GetMapping("/exercises")
    ExerciseListResponse listExercises(
            @Valid ListExercisesRequest request, Locale locale, Authentication authentication) {
        return mapper.toResponse(
                listExercises.list(new ListExercisesQuery(
                        signedIn(authentication), request.category(), request.name(), locale)),
                locale);
    }

    /**
     * Adds an exercise owned by the trainer. Refused with {@code 409} when an exercise in the trainer's library has the
     * name, ignoring letter case, or when the trainer already owns the maximum number of exercises.
     */
    @PostMapping("/exercises")
    @ResponseStatus(HttpStatus.CREATED)
    ExerciseResponse createExercise(
            @Valid @RequestBody CreateExerciseRequest request, Locale locale, Authentication authentication) {
        return mapper.toResponse(manageExercises.create(mapper.toCommand(signedIn(authentication), request)), locale);
    }

    /**
     * Replaces the values of the trainer's own exercise; its measure stays. {@code 404} unless the exercise is the
     * trainer's own, {@code 409} on a name clash like when adding.
     */
    @PutMapping("/exercises/{id}")
    ExerciseResponse updateExercise(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateExerciseRequest request,
            Locale locale,
            Authentication authentication) {
        return mapper.toResponse(
                manageExercises.update(mapper.toCommand(signedIn(authentication), new ExerciseId(id), request)),
                locale);
    }
}
