package com.kinetiq.quiz.domain.service;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.quiz.domain.exception.QuizAlreadySubmittedException;
import com.kinetiq.quiz.domain.input.SubmitQuizCommand;
import com.kinetiq.quiz.domain.input.SubmitQuizUseCase;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.quiz.domain.model.QuizStatus;
import com.kinetiq.quiz.domain.model.QuizSubmission;
import com.kinetiq.quiz.domain.model.QuizSubmissionId;
import com.kinetiq.quiz.domain.model.QuizSubmitted;
import com.kinetiq.quiz.domain.output.QuizEventPublisher;
import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import java.time.Clock;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class QuizSubmissionService implements SubmitQuizUseCase, ViewQuizAnswersUseCase {

    private final QuizSubmissionRepository submissions;
    private final QuizEventPublisher events;
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
        events.submitted(new QuizSubmitted(command.client()));
        return QuizStatus.DRAFTING_PLAN;
    }

    @Override
    public QuizStatus status(AccountId client) {
        if (!submissions.existsByClient(client)) {
            return QuizStatus.NOT_SUBMITTED;
        }
        if (submissions.hasPublishedPlan(client)) {
            return QuizStatus.PLAN_PUBLISHED;
        }
        return submissions.isPlanDrafting(client) ? QuizStatus.DRAFTING_PLAN : QuizStatus.WAITING_FOR_PLAN;
    }

    @Override
    public Optional<QuizSubmission> answers(AccountId client) {
        return submissions.findByClient(client);
    }
}
