package com.kinetiq.plan.infrastructure.config;

import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.plan.domain.input.ManagePlanUseCase;
import com.kinetiq.plan.domain.output.PlanRepository;
import com.kinetiq.plan.domain.service.PlanManagementService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PlanConfig {

    @Bean
    ManagePlanUseCase managePlanUseCase(
            ViewClientsUseCase clients, ListExercisesUseCase exercises, PlanRepository plans) {
        return new PlanManagementService(clients, exercises, plans, Clock.systemUTC());
    }
}
