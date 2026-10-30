package com.kinetiq.quiz.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.input.SubmitQuizCommand;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface QuizWebMapper {

    default SubmitQuizCommand toCommand(AccountId client, SubmitQuizRequest request) {
        return new SubmitQuizCommand(
                client,
                request.goal(),
                request.experience(),
                request.level(),
                request.age(),
                request.heightCm(),
                request.weightKg(),
                request.likes(),
                request.daysPerWeek(),
                request.notes());
    }
}
