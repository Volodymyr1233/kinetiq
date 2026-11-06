package com.kinetiq.quiz.infrastructure.config;

import com.kinetiq.quiz.domain.output.QuizSubmissionRepository;
import com.kinetiq.quiz.domain.service.QuizSubmissionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class QuizConfig {

    @Bean
    QuizSubmissionService quizSubmissionService(QuizSubmissionRepository submissions) {
        return new QuizSubmissionService(submissions, Clock.systemUTC());
    }
}
