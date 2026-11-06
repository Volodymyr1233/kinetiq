package com.kinetiq.common.infrastructure.output;

import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import jakarta.annotation.PreDestroy;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Delivers mail through the Resend HTTPS API. Delivery runs off the request thread, so response times do not reveal
 * whether an email has an account.
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${kinetiq.mail.api-key:}'.isBlank()")
class ResendMailSender implements MailSender {

    private static final String ENDPOINT = "https://api.resend.com/emails";

    private final RestClient http;
    private final MessageSource messages;
    private final String apiKey;
    private final String from;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    ResendMailSender(
            MessageSource messages,
            @Value("${kinetiq.mail.api-key}") String apiKey,
            @Value("${kinetiq.mail.from}") String from) {
        this(RestClient.builder().requestFactory(requestFactory()).build(), messages, apiKey, from);
    }

    ResendMailSender(RestClient http, MessageSource messages, String apiKey, String from) {
        if (from.isBlank()) {
            throw new IllegalStateException("MAIL_FROM must be set when MAIL_API_KEY is set");
        }
        this.http = http;
        this.messages = messages;
        this.apiKey = apiKey;
        this.from = from;
    }

    // the JDK client waits forever unless told otherwise
    private static JdkClientHttpRequestFactory requestFactory() {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build());
        factory.setReadTimeout(Duration.ofSeconds(10));
        return factory;
    }

    @Override
    public void send(Mail mail) {
        Map<String, Object> body = Map.of(
                "from", from,
                "to", List.of(mail.to().value()),
                "subject", messages.getMessage("mail." + mail.template() + ".subject", null, mail.locale()),
                "text", text(mail));
        executor.execute(() -> deliver(body, mail.template()));
    }

    private String text(Mail mail) {
        String text = messages.getMessage("mail." + mail.template() + ".body", null, mail.locale());
        for (Map.Entry<String, String> value : mail.values().entrySet()) {
            text = text.replace("{" + value.getKey() + "}", value.getValue());
        }
        return text;
    }

    // ponytail: a failed mail is logged and lost; add a retry or an outbox when losing one hurts
    private void deliver(Map<String, Object> body, String template) {
        try {
            http.post()
                    .uri(ENDPOINT)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            // the response body can echo the recipient: log the status only
            log.warn("Mail not delivered, template={}, status={}", template, exception.getStatusCode().value());
        } catch (RestClientException exception) {
            log.warn("Mail not delivered, template={}, cause={}", template, exception.getClass().getSimpleName());
        }
    }

    /** Waits for the mails still in flight, so a shutdown does not drop them. */
    @PreDestroy
    void destroy() {
        executor.close();
    }
}
