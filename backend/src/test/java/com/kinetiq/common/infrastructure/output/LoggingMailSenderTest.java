package com.kinetiq.common.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.ResourceBundleMessageSource;

@ExtendWith(OutputCaptureExtension.class)
class LoggingMailSenderTest {

    private static final String LINK = "http://localhost:5173/confirm-email#token=abc";

    private static Mail mail(Locale locale) {
        return new Mail(new Email("marek@example.com"), "confirm-email", locale, Map.of("link", LINK));
    }

    private static LoggingMailSender sender(boolean logContent) {
        ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        return new LoggingMailSender(messages, logContent);
    }

    @Test
    void shouldLogRecipientSubjectAndLink_whenContentLoggingIsOn(CapturedOutput output) {
        sender(true).send(mail(Locale.ENGLISH));

        assertThat(output).contains("marek@example.com", "Confirm your Kinetiq account", LINK);
    }

    @Test
    void shouldWriteSubjectInPolish_whenMailIsInPolish(CapturedOutput output) {
        sender(true).send(mail(Locale.forLanguageTag("pl")));

        assertThat(output).contains("Potwierdź konto w Kinetiq");
    }

    @Test
    void shouldLogOnlyTemplate_whenContentLoggingIsOff(CapturedOutput output) {
        sender(false).send(mail(Locale.ENGLISH));

        assertThat(output).contains("confirm-email").doesNotContain("marek@example.com", "token=");
    }
}
