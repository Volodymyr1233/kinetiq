package com.kinetiq.common.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.MessageSource;

class MailSenderSelectionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withBean(MessageSource.class, () -> mock(MessageSource.class))
            .withUserConfiguration(LoggingMailSender.class, ResendMailSender.class)
            .withPropertyValues("kinetiq.mail.from=no-reply@kinetiq.example");

    @Test
    void shouldKeepLoggingAdapter_whenApiKeyIsEmpty() {
        runner.withPropertyValues("kinetiq.mail.api-key=").run(context -> assertThat(context)
                .hasSingleBean(LoggingMailSender.class)
                .doesNotHaveBean(ResendMailSender.class));
    }

    @Test
    void shouldUseResendAdapter_whenApiKeyIsSet() {
        runner.withPropertyValues("kinetiq.mail.api-key=re_test").run(context -> assertThat(context)
                .hasSingleBean(ResendMailSender.class)
                .doesNotHaveBean(LoggingMailSender.class));
    }
}
