package com.kinetiq.planchange.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.planchange.domain.input.DetectPlanChangesUseCase;
import com.kinetiq.planchange.domain.input.ViewPlanChangeUseCase;
import com.kinetiq.planchange.domain.output.PlanChangeDrafter;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import com.kinetiq.planchange.domain.output.PlanSource;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import com.kinetiq.planchange.domain.rule.HighRpeFallback;
import com.kinetiq.planchange.domain.rule.HighRpeLimits;
import com.kinetiq.planchange.domain.rule.HighRpeRule;
import com.kinetiq.planchange.domain.rule.LowCompletionFallback;
import com.kinetiq.planchange.domain.rule.LowCompletionLimits;
import com.kinetiq.planchange.domain.rule.LowCompletionRule;
import com.kinetiq.planchange.domain.rule.StalledWeightFallback;
import com.kinetiq.planchange.domain.rule.StalledWeightLimits;
import com.kinetiq.planchange.domain.rule.StalledWeightRule;
import com.kinetiq.planchange.domain.service.PlanChangeDetectionService;
import com.kinetiq.planchange.domain.service.PlanChangeDraftingService;
import com.kinetiq.planchange.domain.service.PlanChangeLimits;
import com.kinetiq.planchange.domain.service.PlanChangeViewService;
import com.kinetiq.planchange.infrastructure.output.OpenRouterPlanChangeDrafter;
import com.kinetiq.planchange.infrastructure.output.UnavailablePlanChangeDrafter;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration(proxyBeanMethods = false)
class PlanChangeConfig {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1";

    @Bean
    DetectPlanChangesUseCase detectPlanChangesUseCase(
            TrainingHistory history, PlanChangeRepository planChanges, PlanSource plans, PlanChangeDrafter drafter) {
        PlanChangeDraftingService drafting = new PlanChangeDraftingService(
                drafter,
                new PlanChangeLimits(List.of(new LowCompletionLimits(), new HighRpeLimits(), new StalledWeightLimits())),
                List.of(new LowCompletionFallback(), new HighRpeFallback(), new StalledWeightFallback()));
        return new PlanChangeDetectionService(
                history,
                planChanges,
                List.of(new LowCompletionRule(), new HighRpeRule(), new StalledWeightRule()),
                plans,
                drafting,
                Clock.systemUTC());
    }

    @Bean
    ViewPlanChangeUseCase viewPlanChangeUseCase(
            ViewClientsUseCase clients,
            CurrentAccountUseCase accounts,
            PlanChangeRepository planChanges,
            PlanSource plans) {
        return new PlanChangeViewService(clients, accounts, planChanges, plans);
    }

    // the chat model is the drafter's own, not a bean, so it cannot be mistaken for the plan drafting one
    // no retries in the client: one attempt is one request
    @Bean
    @ConditionalOnExpression("!'${kinetiq.plan-drafting.api-key:}'.isBlank()")
    PlanChangeDrafter openRouterPlanChangeDrafter(
            @Value("${kinetiq.plan-drafting.api-key}") String apiKey,
            @Value("${kinetiq.plan-drafting.model}") String model,
            @Value("${kinetiq.plan-drafting.timeout}") Duration timeout,
            @Value("${kinetiq.plan-drafting.max-tokens}") int maxTokens,
            @Value("${kinetiq.plan-drafting.temperature}") double temperature,
            @Value("${kinetiq.plan-drafting.reasoning-effort}") String reasoningEffort) {
        return new OpenRouterPlanChangeDrafter(OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(OPENROUTER_URL)
                        .apiKey(apiKey)
                        .model(model)
                        .timeout(timeout)
                        .maxRetries(0)
                        .maxTokens(maxTokens)
                        .temperature(temperature)
                        .extraBody(Map.of(
                                "provider", Map.of("require_parameters", true),
                                "reasoning", Map.of("effort", reasoningEffort, "exclude", true)))
                        .outputSchema(OpenRouterPlanChangeDrafter.ANSWER_SCHEMA)
                        .build())
                .build());
    }

    @Bean
    @ConditionalOnExpression("'${kinetiq.plan-drafting.api-key:}'.isBlank()")
    PlanChangeDrafter unavailablePlanChangeDrafter() {
        log.warn("No drafting service is configured, so every plan change gets its rule's fixed transformation");
        return new UnavailablePlanChangeDrafter();
    }
}
