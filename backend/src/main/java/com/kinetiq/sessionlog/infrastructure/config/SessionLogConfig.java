package com.kinetiq.sessionlog.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.sessionlog.domain.input.LogSessionUseCase;
import com.kinetiq.sessionlog.domain.input.ViewSessionStatesUseCase;
import com.kinetiq.sessionlog.domain.output.SessionLogRepository;
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
}
