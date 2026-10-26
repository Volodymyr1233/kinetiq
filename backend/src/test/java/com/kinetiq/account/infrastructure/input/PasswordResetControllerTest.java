package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.kinetiq.account.domain.exception.PasswordResetLinkInvalidException;
import com.kinetiq.account.domain.input.ResetPasswordUseCase;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(PasswordResetController.class)
@Import({SecurityConfig.class, WebConfig.class})
class PasswordResetControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ResetPasswordUseCase resetPassword;

    @MockitoBean
    private RequestLimiter limiter;

    @BeforeEach
    void allowEveryAddress() {
        when(limiter.tryAcquire(any(), any())).thenReturn(true);
    }

    private MvcTestResult post(String uri, Object body) {
        return mvc.post()
                .uri(uri)
                .with(csrf())
                .header("Accept-Language", "pl-PL,pl;q=0.9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body))
                .exchange();
    }

    @Nested
    class RequestReset {

        @Test
        void shouldAcceptAndPassNormalisedEmail_whenCallerIsAnonymous() {
            var result = post("/api/v1/password-reset-links", Map.of("email", " Marek@Example.com "));

            assertThat(result).hasStatus(HttpStatus.ACCEPTED);
            verify(resetPassword).requestReset(new Email("marek@example.com"), Locale.forLanguageTag("pl"));
        }

        @Test
        void shouldAcceptWithoutSending_whenAddressAskedTooOften() {
            when(limiter.tryAcquire(RequestLimiter.Kind.PASSWORD_RESET, "127.0.0.1"))
                    .thenReturn(false);

            var result = post("/api/v1/password-reset-links", Map.of("email", "marek@example.com"));

            assertThat(result).hasStatus(HttpStatus.ACCEPTED);
            verify(resetPassword, never()).requestReset(any(), any());
        }

        @Test
        void shouldReportPattern_whenEmailIsMalformed() {
            var result = post("/api/v1/password-reset-links", Map.of("email", "marek@example"));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .contains(Map.of("field", "email", "code", "pattern"));
            verify(resetPassword, never()).requestReset(any(), any());
        }
    }

    @Nested
    class SetPassword {

        @Test
        void shouldPassTokenAndPassword_whenRequestIsValid() {
            var result = post("/api/v1/password-resets", Map.of("token", "abc", "password", "New-pass-2026"));

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            verify(resetPassword).setPassword("abc", new RawPassword("New-pass-2026"));
        }

        @Test
        void shouldReportStableCode_whenLinkIsInvalid() {
            doThrow(new PasswordResetLinkInvalidException()).when(resetPassword).setPassword(any(), any());

            var result = post("/api/v1/password-resets", Map.of("token", "abc", "password", "New-pass-2026"));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.password-reset-link-invalid");
        }

        @ParameterizedTest
        @CsvSource(
                delimiter = '|',
                value = {
                    "password | 'Kin-26a'      | size",
                    "password | 'kinetiq-2026' | pattern",
                    "token    | ''            | not-blank",
                })
        void shouldReportFieldCode_whenFieldBreaksRule(String field, String value, String code) {
            var body = new java.util.HashMap<>(Map.of("token", "abc", "password", "New-pass-2026"));
            body.put(field, value);

            var result = post("/api/v1/password-resets", body);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .contains(Map.of("field", field, "code", code));
            verify(resetPassword, never()).setPassword(any(), any());
        }
    }
}
