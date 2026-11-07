package com.kinetiq.client.infrastructure.config;

import com.kinetiq.client.domain.input.ReviewClientAnswersUseCase;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.client.domain.service.ClientAnswersReviewService;
import com.kinetiq.client.domain.service.ClientViewService;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClientConfig {

    @Bean
    ViewClientsUseCase viewClientsUseCase(ClientDirectory clients) {
        return new ClientViewService(clients);
    }

    @Bean
    ReviewClientAnswersUseCase reviewClientAnswersUseCase(ClientDirectory clients, ViewQuizAnswersUseCase quiz) {
        return new ClientAnswersReviewService(clients, quiz);
    }
}
