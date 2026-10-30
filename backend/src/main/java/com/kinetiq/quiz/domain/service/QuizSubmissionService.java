package com.kinetiq.quiz.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.input.SubmitQuizCommand;
import com.kinetiq.quiz.domain.input.SubmitQuizUseCase;
import com.kinetiq.quiz.domain.model.QuizStatus;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuizSubmissionService implements SubmitQuizUseCase {

    private final QuizSubmissionRepository submissions;
    private final Clock clock;

    // the repository refuses a submit that raced past this check
    @Override
    public QuizStatus submit(SubmitQuizCommand command) {
        if (submissions.existsByClient(command.client())) {
            throw new QuizAlreadySubmittedException();
        }
        submissions.save(new QuizSubmission(
                QuizSubmissionId.random(),
                command.client(),
                command.goal(),
                command.experience(),
                command.level(),
                command.age(),
                command.heightCm(),
                command.weightKg(),
                command.likes(),
                command.daysPerWeek(),
                command.notes(),
                clock.instant()));
        return QuizStatus.WAITING_FOR_PLAN;
    }

    // plans arrive with the plan stories, which add PLAN_PUBLISHED
    @Override
    public QuizStatus status(AccountId client) {
        return submissions.existsByClient(client) ? QuizStatus.WAITING_FOR_PLAN : QuizStatus.NOT_SUBMITTED;
    }
}
