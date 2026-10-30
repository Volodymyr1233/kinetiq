package com.kinetiq.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class LlmConfigTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(LlmConfig.class)
            .withPropertyValues(
                    "kinetiq.llm.base-url=http://localhost:1/v1", "kinetiq.llm.model=test-model", "kinetiq.llm.timeout=PT5S");

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void shouldStartWithoutLanguageModel_whenKeyIsEmptyOrSpaces(String key) {
        runner.withPropertyValues("kinetiq.llm.api-key=" + key).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ChatModel.class);
        });
    }

    @Test
    void shouldStartWithoutLanguageModel_whenKeyIsNotSetAtAll() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(ChatModel.class);
        });
    }

    @Test
    void shouldProvideOneLanguageModel_whenKeyIsSet() {
        runner.withPropertyValues("kinetiq.llm.api-key=test-key").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(ChatModel.class);
        });
    }
}
