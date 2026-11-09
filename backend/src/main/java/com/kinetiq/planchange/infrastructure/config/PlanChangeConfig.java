package com.kinetiq.planchange.infrastructure.config;

import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import com.kinetiq.planchange.domain.service.PlanChangeDetectionService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PlanChangeConfig {

    @Bean
    DetectPlanChangesUseCase detectPlanChangesUseCase(TrainingHistory history) {
        return new PlanChangeDetectionService(history, Clock.systemUTC());
    }
}
