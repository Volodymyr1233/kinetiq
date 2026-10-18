package com.kinetiq.common.infrastructure.output;

import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

/** Logs instead of delivering until real delivery exists. Locally the link can be copied from the log. */
@Slf4j
@Component
class LoggingMailSender implements MailSender {

    private final MessageSource messages;
    private final boolean logContent;

    // recipient and link are personal data and a way into an account: only the local profile logs them
    LoggingMailSender(MessageSource messages, @Value("${kinetiq.mail.log-content:false}") boolean logContent) {
        this.messages = messages;
        this.logContent = logContent;
    }

    @Override
    public void send(Mail mail) {
        if (!logContent) {
            log.info("Mail not delivered, template={}", mail.template());
            return;
        }
        String subject = messages.getMessage("mail." + mail.template() + ".subject", null, mail.locale());
        log.info(
                "Mail not delivered, to={} subject=\"{}\" link={}",
                mail.to().value(),
                subject,
                mail.values().get("link"));
    }
}
