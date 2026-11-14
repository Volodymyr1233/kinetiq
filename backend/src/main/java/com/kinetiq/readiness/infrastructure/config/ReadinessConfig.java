package com.kinetiq.readiness.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.common.domain.output.NoticeBoard;
import com.kinetiq.plan.domain.input.ViewOwnPlanUseCase;
import com.kinetiq.readiness.domain.input.CheckInUseCase;
import com.kinetiq.readiness.domain.output.CheckInRepository;
import com.kinetiq.readiness.domain.output.TrainingProgress;
import com.kinetiq.readiness.domain.service.CheckInService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ReadinessConfig {

    @Bean
    CheckInUseCase checkInUseCase(
            ViewOwnPlanUseCase plan,
            TrainingProgress training,
            CheckInRepository checkIns,
            CurrentAccountUseCase accounts,
            NoticeBoard notices) {
        return new CheckInService(plan, training, checkIns, accounts, notices, Clock.systemUTC());
    }
}
