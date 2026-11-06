package com.kinetiq.quiz.infrastructure.output;

import com.kinetiq.quiz.domain.model.QuizSubmitted;
import com.kinetiq.quiz.domain.output.QuizEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class SpringQuizEventPublisher implements QuizEventPublisher {

    private final ApplicationEventPublisher events;

    @Override
    public void submitted(QuizSubmitted event) {
        events.publishEvent(event);
    }
}
