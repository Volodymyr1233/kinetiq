package com.kinetiq.progress.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.progress.domain.input.ViewProgressUseCase;
import com.kinetiq.progress.domain.output.TrainingLog;
import com.kinetiq.progress.domain.service.ProgressViewService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ProgressConfig {

    @Bean
    ViewProgressUseCase viewProgressUseCase(
            ViewClientsUseCase clients,
            ViewOwnPlanUseCase plans,
            CurrentAccountUseCase accounts,
            TrainingLog trainingLog) {
        return new ProgressViewService(clients, plans, accounts, trainingLog, Clock.systemUTC());
    }
}
