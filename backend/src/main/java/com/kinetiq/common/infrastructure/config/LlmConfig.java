package com.kinetiq.common.infrastructure.config;

import java.time.Duration;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The language model Kinetiq drafts with. It exists only when a key is set, so the backend starts without one. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(LlmConfig.Settings.class)
class LlmConfig {

    @ConfigurationProperties("kinetiq.llm")
    record Settings(String apiKey, String baseUrl, String model, Duration timeout) {

        // a record prints every field, and the key must not reach a log
        @Override
        public String toString() {
            return "Settings[baseUrl=" + baseUrl + ", model=" + model + ", timeout=" + timeout + "]";
        }
    }

    // built here and not by Spring AI's starter, which refuses to start without a key
    @Bean
    @ConditionalOnExpression("!'${kinetiq.llm.api-key:}'.isBlank()")
    ChatModel llmChatModel(Settings settings) {
        return OpenAiChatModel.builder()
                .options(OpenAiChatOptions.builder()
                        .apiKey(settings.apiKey().strip())
                        .baseUrl(settings.baseUrl())
                        .model(settings.model())
                        .timeout(settings.timeout())
                        // the two attempts of a draft are counted in the domain, so no hidden retries here
                        .maxRetries(0)
                        .build())
                .build();
    }
}
