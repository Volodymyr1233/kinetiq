package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.exception.CredentialsInvalidException;
import com.kinetiq.account.domain.exception.EmailNotConfirmedException;
import com.kinetiq.account.domain.exception.SignInLockedException;
import com.kinetiq.account.domain.input.SignInCommand;
import com.kinetiq.account.domain.input.SignInUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.time.Duration;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(SignInController.class)
@Import({SecurityConfig.class, WebConfig.class, AccountWebMapperImpl.class})
class SignInControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final UUID ID = UUID.fromString("00000000-0000-4000-8000-000000000001");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SignInUseCase signIn;

    private static Account trainer() {
        return Account.restore(
                new AccountId(ID),
                "Marek Nowak",
                new Email("marek@example.com"),
                "hash",
                Role.TRAINER,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null);
    }

    private MvcTestResult signIn(Object body, MockHttpSession session) {
        return mvc.post()
                .uri("/api/v1/sign-ins")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body))
                .exchange();
    }

    private MvcTestResult signIn(Object body) {
        return signIn(body, new MockHttpSession());
    }

    @Nested
    class SignIn {

        @Test
        void shouldAnswerAccountAndStoreSignIn_whenCredentialsMatch() {
            when(signIn.signIn(new SignInCommand("Marek@Example.com", "Kinetiq-2026"))).thenReturn(trainer());

            var result = signIn(Map.of("email", "Marek@Example.com", "password", "Kinetiq-2026"));

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ID.toString());
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Marek Nowak");
            assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("TRAINER");
            assertThat(result).bodyJson().extractingPath("$.timeZone").isEqualTo("Europe/Warsaw");
            assertThat(result).bodyJson().doesNotHavePath("$.email");
            var context = (SecurityContext) result.getRequest()
                    .getSession()
                    .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
            assertThat(context.getAuthentication().getName()).isEqualTo(ID.toString());
            assertThat(context.getAuthentication().getAuthorities())
                    .extracting(Object::toString)
                    .containsExactly("ROLE_TRAINER");
        }

        @Test
        void shouldReplaceSessionId_whenSignInSucceeds() {
            when(signIn.signIn(any())).thenReturn(trainer());
            MockHttpSession before = new MockHttpSession();
            String idBefore = before.getId();

            var result = signIn(Map.of("email", "marek@example.com", "password", "Kinetiq-2026"), before);

            assertThat(result).hasStatusOk();
            assertThat(result.getRequest().getSession().getId()).isNotEqualTo(idBefore);
        }

        @Test
        void shouldAnswerUnauthorized_whenCredentialsAreWrong() {
            when(signIn.signIn(any())).thenThrow(new CredentialsInvalidException());

            var result = signIn(Map.of("email", "nobody@example.com", "password", "Kinetiq-2026"));

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.credentials-invalid");
            assertThat(result.getRequest().getSession(false))
                    .satisfiesAnyOf(
                            session -> assertThat(session).isNull(),
                            session -> assertThat(session.getAttribute(
                                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                                    .isNull());
        }

        @Test
        void shouldAnswerForbidden_whenTrainerEmailIsNotConfirmed() {
            when(signIn.signIn(any())).thenThrow(new EmailNotConfirmedException());

            var result = signIn(Map.of("email", "marek@example.com", "password", "Kinetiq-2026"));

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.email-not-confirmed");
        }

        @Test
        void shouldAnswerTooManyRequestsWithTimeLeft_whenEmailIsLocked() {
            when(signIn.signIn(any())).thenThrow(new SignInLockedException(Duration.ofSeconds(839, 1)));

            var result = signIn(Map.of("email", "marek@example.com", "password", "Kinetiq-2026"));

            assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.sign-in-locked");
            assertThat(result).bodyJson().extractingPath("$.retryAfterSeconds").isEqualTo(840);
        }

        @Test
        void shouldListBothFields_whenTheyAreBlank() {
            var result = signIn(Map.of("email", " ", "password", ""));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .containsExactlyInAnyOrder(
                            Map.of("field", "email", "code", "not-blank"),
                            Map.of("field", "password", "code", "not-blank"));
            verify(signIn, never()).signIn(any());
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.post()
                    .uri("/api/v1/sign-ins")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"marek@example.com\",\"password\":\"Kinetiq-2026\"}")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
            verify(signIn, never()).signIn(any());
        }
    }

    @Nested
    class SignOut {

        @Test
        void shouldEndSession_whenSignedIn() {
            MockHttpSession session = new MockHttpSession();

            var result = mvc.delete()
                    .uri("/api/v1/sign-ins/current")
                    .with(csrf())
                    .with(user(ID.toString()).roles("TRAINER"))
                    .session(session)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(session.isInvalid()).isTrue();
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = mvc.delete().uri("/api/v1/sign-ins/current").with(csrf()).exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        }
    }
}
