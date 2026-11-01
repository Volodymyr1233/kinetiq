package com.kinetiq.common.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

@ExtendWith(OutputCaptureExtension.class)
class ResendMailSenderTest {

    private static final String ENDPOINT = "https://api.resend.com/emails";
    private static final String LINK = "https://kinetiq.example/confirm-email#token=abc";
    private static final String FROM = "Kinetiq <no-reply@kinetiq.example>";

    private final ResourceBundleMessageSource messages = new ResourceBundleMessageSource();
    private MockRestServiceServer server;
    private ResendMailSender sender;

    @BeforeEach
    void setUp() {
        messages.setBasename("messages");
        messages.setDefaultEncoding("UTF-8");
        messages.setFallbackToSystemLocale(false);
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        sender = new ResendMailSender(builder.build(), messages, "re_test", FROM);
    }

    private static Mail mail(Locale locale) {
        return new Mail(new Email("marek@example.com"), "confirm-email", locale, Map.of("link", LINK));
    }

    @Test
    void shouldPostPolishMessage_whenMailIsPolish() {
        server.expect(requestTo(ENDPOINT))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer re_test"))
                .andExpect(jsonPath("$.from").value(FROM))
                .andExpect(jsonPath("$.to[0]").value("marek@example.com"))
                .andExpect(jsonPath("$.subject").value("Potwierdź konto w Kinetiq"))
                .andExpect(jsonPath("$.text").value(containsString(LINK)))
                .andRespond(withSuccess("{\"id\":\"1\"}", MediaType.APPLICATION_JSON));

        sender.send(mail(Locale.forLanguageTag("pl")));
        sender.destroy();

        server.verify();
    }

    @Test
    void shouldNotWaitForDelivery_whenServiceIsSlow() {
        CountDownLatch release = new CountDownLatch(1);
        server.expect(requestTo(ENDPOINT)).andRespond(request -> {
            try {
                release.await();
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
            return new MockClientHttpResponse(new byte[0], HttpStatus.OK);
        });

        assertTimeoutPreemptively(Duration.ofSeconds(2), () -> sender.send(mail(Locale.ENGLISH)));

        release.countDown();
        sender.destroy();
        server.verify();
    }

    @Test
    void shouldLogOnlyTemplateAndStatus_whenDeliveryFails(CapturedOutput output) {
        server.expect(requestTo(ENDPOINT)).andRespond(withServerError());

        sender.send(mail(Locale.ENGLISH));
        sender.destroy();

        assertThat(output).contains("confirm-email", "500").doesNotContain("marek@example.com", "re_test");
    }

    @Test
    void shouldRefuseToStart_whenSenderAddressIsBlank() {
        assertThatThrownBy(() -> new ResendMailSender(RestClient.create(), messages, "re_test", " "))
                .isInstanceOf(IllegalStateException.class);
    }
}
