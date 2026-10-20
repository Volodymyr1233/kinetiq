package com.kinetiq.exercise.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ExerciseMapper {

    // the aggregate validates in its constructor; nothing saves exercises before story #47
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
}
