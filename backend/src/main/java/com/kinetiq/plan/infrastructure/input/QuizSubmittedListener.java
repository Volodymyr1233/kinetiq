package com.kinetiq.plan.infrastructure.input;

import com.kinetiq.plan.domain.input.DraftPlanUseCase;
import com.kinetiq.quiz.domain.model.QuizSubmitted;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Starts drafting from a submit: recorded with the answers, and running only once they are safely stored. */
@Component
@RequiredArgsConstructor
class QuizSubmittedListener {

    private final DraftPlanUseCase drafting;
    private final DraftingRunner runner;

    @EventListener
    void record(QuizSubmitted event) {
        drafting.request(event.client());
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void start(QuizSubmitted event) {
        runner.start(event.client());
    }
}
