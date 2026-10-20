package com.kinetiq.exercise.infrastructure.input;

import com.kinetiq.exercise.domain.input.ExerciseListing;
import com.kinetiq.exercise.domain.model.Exercise;
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

    private ExerciseResponse toResponse(ExerciseListing.Entry entry, Locale locale) {
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
                entry.plansUsing());
    }
}
