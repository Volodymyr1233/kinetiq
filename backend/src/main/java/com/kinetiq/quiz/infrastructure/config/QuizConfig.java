package com.kinetiq.quiz.infrastructure.config;

import com.kinetiq.quiz.domain.input.SubmitQuizUseCase;
import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import com.kinetiq.quiz.domain.service.QuizSubmissionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class QuizConfig {

    @Bean
    SubmitQuizUseCase submitQuizUseCase(QuizSubmissionRepository submissions) {
        return new QuizSubmissionService(submissions, Clock.systemUTC());
    }
}
