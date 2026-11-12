package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.exception.PasswordIncorrectException;
import com.kinetiq.account.domain.exception.SignInLockedException;
import com.kinetiq.account.domain.exception.TrainerHasClientsException;
import com.kinetiq.account.domain.input.DeleteAccountUseCase;
import com.kinetiq.account.domain.input.DeletionCheck;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(AccountDeletionController.class)
@Import({SecurityConfig.class, WebConfig.class})
class AccountDeletionControllerTest {

    private static final AccountId ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final RequestPostProcessor CLIENT = user(ID.value().toString()).roles("CLIENT");
    private static final String URI = "/api/v1/accounts/me/deletion";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private DeleteAccountUseCase deletion;

    private MvcTestResult delete(String body) {
        return mvc.post()
                .uri(URI)
                .with(csrf())
                .with(CLIENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Nested
    class Check {

        @Test
        void shouldAnswerTheTrainersName_forAClient() {
            when(deletion.check(ID)).thenReturn(new DeletionCheck(0, "Marek Nowak"));

            var result = mvc.get().uri(URI).with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().isEqualTo("{\"clients\": 0, \"trainerName\": \"Marek Nowak\"}");
        }

        @Test
        void shouldLeaveOutTheTrainersName_forATrainer() {
            when(deletion.check(ID)).thenReturn(new DeletionCheck(3, null));

            var result = mvc.get().uri(URI).with(CLIENT).exchange();

            assertThat(result).bodyJson().isStrictlyEqualTo("{\"clients\": 3}");
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = mvc.get().uri(URI).exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    class Delete {

        @Test
        void shouldDeleteAndEndThisSignIn() {
            MockHttpSession session = new MockHttpSession();

            var result = mvc.post()
                    .uri(URI)
                    .with(csrf())
                    .with(CLIENT)
                    .session(session)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\": \"Kinetiq-2026\"}")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            verify(deletion).delete(ID, "Kinetiq-2026");
            assertThat(session.isInvalid()).isTrue();
        }

        @Test
        void shouldRefuseABlankPassword_withoutCallingTheUseCase() {
            var result = delete("{\"password\": \" \"}");

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
            assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("password");
            verify(deletion, never()).delete(any(), anyString());
        }

        @Test
        void shouldAnswerPasswordIncorrect() {
            doThrow(new PasswordIncorrectException()).when(deletion).delete(any(), anyString());

            var result = delete("{\"password\": \"Wrong-2026\"}");

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.password-incorrect");
        }

        @Test
        void shouldAnswerLocked_withTheTimeLeft() {
            doThrow(new SignInLockedException(Duration.ofMinutes(14))).when(deletion).delete(any(), anyString());

            var result = delete("{\"password\": \"Kinetiq-2026\"}");

            assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.sign-in-locked");
            assertThat(result).bodyJson().extractingPath("$.retryAfterSeconds").isEqualTo(840);
        }

        @Test
        void shouldAnswerHasClients_withTheirNumber() {
            doThrow(new TrainerHasClientsException(2)).when(deletion).delete(any(), anyString());

            var result = delete("{\"password\": \"Kinetiq-2026\"}");

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.has-clients");
            assertThat(result).bodyJson().extractingPath("$.clients").isEqualTo(2);
        }

        @Test
        void shouldAnswerInternalErrorWithoutDetails_whenSomethingUnexpectedFails() throws Exception {
            doThrow(new IllegalStateException("connection refused to db-host"))
                    .when(deletion)
                    .delete(any(), anyString());

            var result = delete("{\"password\": \"Kinetiq-2026\"}");

            assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain("connection refused", "db-host", "IllegalStateException");
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = mvc.post()
                    .uri(URI)
                    .with(csrf())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\": \"Kinetiq-2026\"}")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            verify(deletion, never()).delete(any(), anyString());
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.post()
                    .uri(URI)
                    .with(CLIENT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"password\": \"Kinetiq-2026\"}")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            verify(deletion, never()).delete(any(), anyString());
        }
    }
}
