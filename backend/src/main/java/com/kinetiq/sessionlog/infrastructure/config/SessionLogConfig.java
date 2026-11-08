package com.kinetiq.sessionlog.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.quiz.domain.input.ViewQuizAnswersUseCase;
import com.kinetiq.sessionlog.domain.input.LogSessionUseCase;
import com.kinetiq.sessionlog.domain.input.ViewClientTrainingUseCase;
import com.kinetiq.sessionlog.domain.input.ViewSessionStatesUseCase;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
import com.kinetiq.sessionlog.domain.service.ClientTrainingViewService;
import com.kinetiq.sessionlog.domain.service.SessionLogService;
import com.kinetiq.sessionlog.domain.service.SessionStateService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class SessionLogConfig {

    @Bean
    LogSessionUseCase logSessionUseCase(
            ViewOwnPlanUseCase plan,
            CurrentAccountUseCase accounts,
            ListExercisesUseCase exercises,
            SessionLogRepository logs) {
        return new SessionLogService(plan, accounts, exercises, logs, Clock.systemUTC());
    }

    @Bean
    ViewSessionStatesUseCase viewSessionStatesUseCase(
            ViewOwnPlanUseCase plan, CurrentAccountUseCase accounts, SessionLogRepository logs) {
        return new SessionStateService(plan, accounts, logs, Clock.systemUTC());
    }

    @Bean
    ViewClientTrainingUseCase viewClientTrainingUseCase(
            ViewClientsUseCase clients,
            ViewQuizAnswersUseCase quiz,
            ViewOwnPlanUseCase plan,
            SessionLogRepository logs) {
        return new ClientTrainingViewService(clients, quiz, plan, logs, Clock.systemUTC());
    }
}
