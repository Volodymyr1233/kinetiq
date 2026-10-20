package com.kinetiq.exercise.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Measure;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "exercises")
class ExerciseEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID ownerId;
    private String name;
    private String namePl;

    @Enumerated(EnumType.STRING)
    private Category category;

    private String equipment;
    private String equipmentPl;

    @Enumerated(EnumType.STRING)
    private Measure measure;

    private int defaultSets;
    private int defaultAmount;
    private int restSeconds;
    private String cues;
    private String cuesPl;
}
