package com.kinetiq.plan.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.exercise.domain.input.ListExercisesUseCase;
import com.kinetiq.plan.domain.input.ManagePlanUseCase;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.plan.domain.output.PlanRepository;
import com.kinetiq.plan.domain.service.PlanManagementService;
import com.kinetiq.plan.domain.service.PlanViewService;
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

    @Bean
    ViewOwnPlanUseCase viewOwnPlanUseCase(
            CurrentAccountUseCase accounts, ListExercisesUseCase exercises, PlanRepository plans) {
        return new PlanViewService(accounts, exercises, plans, Clock.systemUTC());
    }
}
