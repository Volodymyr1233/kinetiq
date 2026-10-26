package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.input.CreateExerciseCommand;
import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.input.UpdateExerciseCommand;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import java.util.Locale;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ExerciseWebMapper {

    // every text picks its own language, so a missing Polish field falls back to English alone
    default ExerciseListResponse toResponse(ExerciseListing listing, Locale locale) {
        return new ExerciseListResponse(
                listing.total(),
                listing.entries().stream().map(entry -> toResponse(entry, locale)).toList());
    }

    default ExerciseResponse toResponse(ExerciseListing.Entry entry, Locale locale) {
        Exercise exercise = entry.exercise();
        return new ExerciseResponse(
                exercise.getId().value(),
                exercise.getName().in(locale),
                exercise.getCategory(),
                exercise.getEquipment().in(locale),
                exercise.getMeasure(),
                exercise.getDefaultSets(),
                exercise.getDefaultAmount(),
                exercise.getRestSeconds(),
                exercise.getCues().in(locale),
                entry.plansUsing(),
                exercise.isStarter());
    }

    default CreateExerciseCommand toCommand(AccountId trainer, CreateExerciseRequest request) {
        return new CreateExerciseCommand(
                trainer,
                request.name(),
                request.category(),
                request.measure(),
                request.equipment(),
                request.sets(),
                request.amount(),
                request.restSeconds(),
                request.cues());
    }

    default UpdateExerciseCommand toCommand(AccountId trainer, ExerciseId id, UpdateExerciseRequest request) {
        return new UpdateExerciseCommand(
                trainer,
                id,
                request.name(),
                request.category(),
                request.equipment(),
                request.sets(),
                request.amount(),
                request.restSeconds(),
                request.cues());
    }
}
