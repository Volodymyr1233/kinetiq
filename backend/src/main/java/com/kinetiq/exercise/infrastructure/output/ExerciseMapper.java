package com.kinetiq.exercise.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ExerciseMapper {

    // the aggregate validates in its constructor, so a row it would refuse fails on reading
    default Exercise toDomain(ExerciseEntity entity) {
        return new Exercise(
                new ExerciseId(entity.getId()),
                entity.getOwnerId() == null ? null : new AccountId(entity.getOwnerId()),
                new LocalizedText(entity.getName(), entity.getNamePl()),
                entity.getCategory(),
                new LocalizedText(entity.getEquipment(), entity.getEquipmentPl()),
                entity.getMeasure(),
                entity.getDefaultSets(),
                entity.getDefaultAmount(),
                entity.getRestSeconds(),
                new LocalizedText(entity.getCues(), entity.getCuesPl()));
    }

    // written by hand because each text splits into two columns, and an empty Polish text is stored as null
    default void update(Exercise exercise, ExerciseEntity entity) {
        entity.setId(exercise.getId().value());
        entity.setOwnerId(exercise.getOwner().map(AccountId::value).orElse(null));
        entity.setName(exercise.getName().en());
        entity.setNamePl(polish(exercise.getName()));
        entity.setCategory(exercise.getCategory());
        entity.setEquipment(exercise.getEquipment().en());
        entity.setEquipmentPl(polish(exercise.getEquipment()));
        entity.setMeasure(exercise.getMeasure());
        entity.setDefaultSets(exercise.getDefaultSets());
        entity.setDefaultAmount(exercise.getDefaultAmount());
        entity.setRestSeconds(exercise.getRestSeconds());
        entity.setCues(exercise.getCues().en());
        entity.setCuesPl(polish(exercise.getCues()));
    }

    private static String polish(LocalizedText text) {
        return text.pl().isEmpty() ? null : text.pl();
    }
}
