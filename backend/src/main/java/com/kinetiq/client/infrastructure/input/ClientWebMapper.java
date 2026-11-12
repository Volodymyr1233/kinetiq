package com.kinetiq.client.infrastructure.input;

import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ClientWebMapper {

    default ClientResponse toResponse(ClientSummary client) {
        return new ClientResponse(
                client.getId().value(),
                client.getName(),
                client.getGoal().orElse(null),
                client.getPlanState());
    }

    // likes in their declared order, so the panel shows them the same way every time
    default QuizAnswersResponse toResponse(QuizSubmission answers) {
        return new QuizAnswersResponse(
                answers.getGoal(),
                answers.getExperience(),
                answers.getLevel(),
                answers.getAge(),
                answers.getHeightCm(),
                answers.getWeightKg(),
                answers.getLikes().stream().sorted().toList(),
                answers.getDaysPerWeek(),
                answers.getNotes().orElse(null),
                answers.getSubmittedAt());
    }
}
