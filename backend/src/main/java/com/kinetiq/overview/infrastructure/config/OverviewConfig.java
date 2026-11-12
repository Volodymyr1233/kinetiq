package com.kinetiq.overview.infrastructure.config;

import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.overview.domain.input.ViewOverviewUseCase;
import com.kinetiq.overview.domain.output.TrainingRecords;
import com.kinetiq.overview.domain.service.OverviewService;
import com.kinetiq.plan.domain.input.ListDraftedPlansUseCase;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class OverviewConfig {

    @Bean
    ViewOverviewUseCase viewOverviewUseCase(
            ViewClientsUseCase clients,
            ListDraftedPlansUseCase draftedPlans,
            ViewPlanChangeUseCase planChanges,
            TrainingRecords trainingRecords) {
        return new OverviewService(clients, draftedPlans, planChanges, trainingRecords, Clock.systemUTC());
    }
}
