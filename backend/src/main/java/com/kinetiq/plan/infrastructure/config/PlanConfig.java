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
import java.time.Duration;
import java.util.Map;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class PlanConfig {

    private static final String OPENROUTER_URL = "https://openrouter.ai/api/v1";

    private static final String ANSWER_SCHEMA =
            """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["sessions"],
              "properties": {
                "sessions": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["name", "day", "entries"],
                    "properties": {
                      "name": { "type": "string" },
                      "day": {
                        "type": "string",
                        "enum": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"]
                      },
                      "entries": {
                        "type": "array",
                        "items": {
                          "type": "object",
                          "additionalProperties": false,
                          "required": ["exercise", "sets", "amount", "loadKg", "rpe", "restSeconds"],
                          "properties": {
                            "exercise": { "type": "string" },
                            "sets": { "type": "integer" },
                            "amount": { "type": "integer" },
                            "loadKg": { "type": ["number", "null"] },
                            "rpe": { "type": "integer" },
                            "restSeconds": { "type": "integer" }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """;

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

    @Bean
    @ConditionalOnExpression("!'${kinetiq.plan-drafting.api-key:}'.isBlank()")
    ChatModel planChatModel(
            @Value("${kinetiq.plan-drafting.api-key}") String apiKey,
            @Value("${kinetiq.plan-drafting.model}") String model,
            @Value("${kinetiq.plan-drafting.timeout}") Duration timeout,
            @Value("${kinetiq.plan-drafting.max-tokens}") int maxTokens,
            @Value("${kinetiq.plan-drafting.temperature}") double temperature,
            @Value("${kinetiq.plan-drafting.reasoning-effort}") String reasoningEffort) {
        return chatModel(
                OPENROUTER_URL, apiKey, new ModelSettings(model, timeout, maxTokens, temperature, reasoningEffort));
    }

    /** What the drafting service is asked to run with, from {@code kinetiq.plan-drafting}. */
    record ModelSettings(String model, Duration timeout, int maxTokens, double temperature, String reasoningEffort) {}

    // no retries in the client: one attempt is one request, and the free allowance counts each
    static ChatModel chatModel(String baseUrl, String apiKey, ModelSettings settings) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(settings.model())
                        .timeout(settings.timeout())
                        .maxRetries(0)
                        .maxTokens(settings.maxTokens())
                        .temperature(settings.temperature())
                        .extraBody(Map.of(
                                "provider", Map.of("require_parameters", true),
                                "reasoning", Map.of("effort", settings.reasoningEffort(), "exclude", true)))
                        .outputSchema(ANSWER_SCHEMA)
                        .build())
                .build();
    }
}
