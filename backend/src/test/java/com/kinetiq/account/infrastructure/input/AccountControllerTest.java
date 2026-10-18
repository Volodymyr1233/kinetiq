package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.kinetiq.account.domain.input.SignUpTrainerCommand;
import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AccountController.class)
@Import({SecurityConfig.class, WebConfig.class})
class AccountControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SignUpUseCase signUp;

    private static Map<String, Object> validSignUp() {
        return Map.of(
                "name", "Marek Nowak",
                "email", "marek@example.com",
                "password", "Kinetiq-2026",
                "adult", true,
                "timeZone", "Europe/Warsaw");
    }

    private static Map<String, Object> with(String field, Object value) {
        var body = new HashMap<>(validSignUp());
        body.put(field, value);
        return body;
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
    class SignUp {

        @Test
        void shouldAcceptAndPassCommand_whenRequestIsValid() {
            var result = post("/api/v1/accounts", with("email", "  Marek@Example.com "));

            assertThat(result).hasStatus(HttpStatus.ACCEPTED);
            ArgumentCaptor<SignUpTrainerCommand> command = ArgumentCaptor.forClass(SignUpTrainerCommand.class);
            verify(signUp).signUpTrainer(command.capture());
            assertThat(command.getValue().email()).isEqualTo(new Email("marek@example.com"));
            assertThat(command.getValue().timeZone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
            assertThat(command.getValue().locale()).isEqualTo(Locale.forLanguageTag("pl"));
            assertThat(command.getValue().password().value()).isEqualTo("Kinetiq-2026");
        }

        @ParameterizedTest
        @CsvSource(
                delimiter = '|',
                value = {
                    "name     | ''                  | not-blank",
                    "email    | 'marek'             | pattern",
                    "email    | ''                  | not-blank",
                    "password | 'Kin-26a'           | size",
                    "password | 'kinetiq-2026'      | pattern",
                    "timeZone | ''                  | not-blank",
                })
        void shouldReportFieldCode_whenFieldBreaksRule(String field, String value, String code) {
            assertValidationFailed(post("/api/v1/accounts", with(field, value)), field, code);
        }

        @Test
        void shouldReportPattern_whenNameHasLineBreak() {
            assertValidationFailed(post("/api/v1/accounts", with("name", "Marek\nNowak")), "name", "pattern");
        }

        @Test
        void shouldReportSize_whenNameIsTooLong() {
            assertValidationFailed(post("/api/v1/accounts", with("name", "a".repeat(101))), "name", "size");
        }

        @Test
        void shouldReportSize_whenEmailIsTooLong() {
            String email = "a".repeat(243) + "@example.com";
            assertValidationFailed(post("/api/v1/accounts", with("email", email)), "email", "size");
        }

        @Test
        void shouldReportNotNull_whenPasswordIsMissing() {
            var body = new HashMap<>(validSignUp());
            body.remove("password");

            assertValidationFailed(post("/api/v1/accounts", body), "password", "not-null");
        }

        @Test
        void shouldReportAssertTrue_whenNotAdult() {
            assertValidationFailed(post("/api/v1/accounts", with("adult", false)), "adult", "assert-true");
        }

        @Test
        void shouldFailValidation_whenTimeZoneIsUnknown() {
            var result = post("/api/v1/accounts", with("timeZone", "Etc/Unknown"));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
            verify(signUp, never()).signUpTrainer(any());
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.post()
                    .uri("/api/v1/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JSON.writeValueAsString(validSignUp()))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
            verify(signUp, never()).signUpTrainer(any());
        }
    }

    private void assertValidationFailed(MvcTestResult result, String field, String code) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
        assertThat(result).bodyJson().extractingPath("$.errors").asArray().contains(Map.of("field", field, "code", code));
        verify(signUp, never()).signUpTrainer(any());
    }
}
