package com.kinetiq.quiz.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import java.util.HashSet;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface QuizSubmissionMapper {

    // written by hand because the typed ids and the optional notes are unwrapped
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

    default QuizSubmission toDomain(QuizSubmissionEntity entity) {
        return new QuizSubmission(
                new QuizSubmissionId(entity.getId()),
                new AccountId(entity.getClientId()),
                entity.getGoal(),
                entity.getExperience(),
                entity.getLevel(),
                entity.getAge(),
                entity.getHeightCm(),
                entity.getWeightKg(),
                entity.getLikes(),
                entity.getDaysPerWeek(),
                entity.getNotes(),
                entity.getSubmittedAt());
    }
}
