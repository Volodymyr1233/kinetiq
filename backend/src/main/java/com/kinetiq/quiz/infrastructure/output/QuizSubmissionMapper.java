package com.kinetiq.quiz.infrastructure.output;

import com.kinetiq.quiz.domain.model.QuizSubmission;
import java.util.HashSet;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface QuizSubmissionMapper {

    // written by hand because the typed ids and the optional notes are unwrapped; nothing reads a submission back yet
    default QuizSubmissionEntity toEntity(QuizSubmission submission) {
        QuizSubmissionEntity entity = new QuizSubmissionEntity();
        entity.setId(submission.getId().value());
        entity.setClientId(submission.getClient().value());
        entity.setGoal(submission.getGoal());
        entity.setExperience(submission.getExperience());
        entity.setLevel(submission.getLevel());
        entity.setAge(submission.getAge());
        entity.setHeightCm(submission.getHeightCm());
        entity.setWeightKg(submission.getWeightKg());
        entity.setLikes(new HashSet<>(submission.getLikes()));
        entity.setDaysPerWeek(submission.getDaysPerWeek());
        entity.setNotes(submission.getNotes().orElse(null));
        entity.setSubmittedAt(submission.getSubmittedAt());
        return entity;
    }
}
