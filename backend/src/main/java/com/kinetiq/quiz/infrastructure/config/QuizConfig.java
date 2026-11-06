package com.kinetiq.quiz.infrastructure.config;

import com.kinetiq.quiz.domain.output.QuizEventPublisher;
import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import com.kinetiq.quiz.domain.service.QuizSubmissionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class QuizConfig {

    @Bean
    QuizSubmissionService quizSubmissionService(QuizSubmissionRepository submissions, QuizEventPublisher events) {
        return new QuizSubmissionService(submissions, events, Clock.systemUTC());
    }
}
