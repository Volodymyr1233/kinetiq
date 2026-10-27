package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.exception.InviteLinkInvalidException;
import com.kinetiq.account.domain.input.InviteDetails;
import com.kinetiq.account.domain.input.JoinCommand;
import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(InviteAcceptanceController.class)
@Import({SecurityConfig.class, WebConfig.class, AccountWebMapperImpl.class, SignIns.class})
class InviteAcceptanceControllerTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AccountId TRAINER_ID = AccountId.random();

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private SignUpUseCase signUp;

    private final Account client = Account.joinAsClient(
            "Anna Kowalska",
            new Email("anna@example.com"),
            "hash",
            ZoneId.of("Europe/Warsaw"),
            AccountSettings.startingWith(Language.PL),
            TRAINER_ID,
            Instant.parse("2026-10-26T10:00:00Z"),
            true);

    private static Map<String, Object> validJoin() {
        return Map.of(
                "token", "secret",
                "name", "Anna Kowalska",
                "password", "Kinetiq-2026",
                "atLeastSixteen", true,
                "healthDataConsent", true,
                "timeZone", "Europe/Warsaw");
    }

    private static Map<String, Object> with(String field, Object value) {
        var body = new HashMap<>(validJoin());
        if (value == null) {
            body.remove(field);
        } else {
            body.put(field, value);
        }
        return body;
    }

    private MvcTestResult post(String uri, Object body, MockHttpSession session, RequestPostProcessor... extra) {
        var request = mvc.post()
                .uri(uri)
                .with(csrf())
                .session(session)
                .header("Accept-Language", "pl-PL,pl;q=0.9")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JSON.writeValueAsString(body));
        for (RequestPostProcessor processor : extra) {
            request.with(processor);
        }
        return request.exchange();
    }

    private MvcTestResult post(String uri, Object body) {
        return post(uri, body, new MockHttpSession());
    }

    private static SecurityContext signedIn(MvcTestResult result) {
        return (SecurityContext) result.getRequest()
                .getSession()
                .getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);
    }

    private static void assertLinkInvalid(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invite.link-invalid");
    }

    @Nested
    class CheckInvite {

        @Test
        void shouldAnswerEmailAndTrainerName_whenAnonymousCallerChecksWorkingLink() {
            when(signUp.checkInvite("secret")).thenReturn(new InviteDetails(new Email("anna@example.com"), "Marek Nowak"));

            var result = post("/api/v1/invite-checks", Map.of("token", "secret"));

            assertThat(result).hasStatusOk();
            assertThat(result)
                    .bodyJson()
                    .isEqualTo("{\"email\":\"anna@example.com\",\"trainerName\":\"Marek Nowak\"}");
        }

        @Test
        void shouldAnswerLinkInvalid_whenLinkDoesNotWork() {
            when(signUp.checkInvite("secret")).thenThrow(new InviteLinkInvalidException());

            assertLinkInvalid(post("/api/v1/invite-checks", Map.of("token", "secret")));
        }

        @Test
        void shouldReportNotBlank_whenTokenIsBlank() {
            var result = post("/api/v1/invite-checks", Map.of("token", " "));

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .contains(Map.of("field", "token", "code", "not-blank"));
            verify(signUp, never()).checkInvite(any());
        }
    }

    @Nested
    class AcceptInvite {

        @Test
        void shouldCreateClientAndSignIn_whenAnonymousCallerJoins() {
            when(signUp.joinAsClient(any())).thenReturn(client);
            MockHttpSession before = new MockHttpSession();
            String idBefore = before.getId();

            var result = post("/api/v1/invite-acceptances", validJoin(), before);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(client.getId().value().toString());
            assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("CLIENT");
            assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("anna@example.com");
            assertThat(result.getRequest().getSession().getId()).isNotEqualTo(idBefore);
            assertThat(signedIn(result).getAuthentication().getName())
                    .isEqualTo(client.getId().value().toString());
            assertThat(signedIn(result).getAuthentication().getAuthorities())
                    .extracting(Object::toString)
                    .containsExactly("ROLE_CLIENT");
        }

        @Test
        void shouldPassTokenNameTimeZoneAndUiLocale_whenJoining() {
            when(signUp.joinAsClient(any())).thenReturn(client);

            post("/api/v1/invite-acceptances", with("name", "  Anna Kowalska "));

            ArgumentCaptor<JoinCommand> command = ArgumentCaptor.forClass(JoinCommand.class);
            verify(signUp).joinAsClient(command.capture());
            assertThat(command.getValue().token()).isEqualTo("secret");
            // the domain trims the name, as at sign-up
            assertThat(command.getValue().name()).isEqualTo("  Anna Kowalska ");
            assertThat(command.getValue().password().value()).isEqualTo("Kinetiq-2026");
            assertThat(command.getValue().timeZone()).isEqualTo(ZoneId.of("Europe/Warsaw"));
            assertThat(command.getValue().locale()).isEqualTo(Locale.forLanguageTag("pl"));
        }

        @Test
        void shouldReplaceEarlierSignIn_whenSignedInTrainerJoins() {
            when(signUp.joinAsClient(any())).thenReturn(client);

            var result = post(
                    "/api/v1/invite-acceptances",
                    validJoin(),
                    new MockHttpSession(),
                    user(TRAINER_ID.value().toString()).roles("TRAINER"));

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(signedIn(result).getAuthentication().getName())
                    .isEqualTo(client.getId().value().toString());
            assertThat(signedIn(result).getAuthentication().getAuthorities())
                    .extracting(Object::toString)
                    .containsExactly("ROLE_CLIENT");
        }

        @ParameterizedTest
        @CsvSource(
                delimiter = '|',
                nullValues = "NULL",
                value = {
                    "token             | NULL          | not-blank",
                    "name              | ''            | not-blank",
                    "password          | 'Kinetiq2026' | pattern",
                    "password          | 'Kin-26a'     | size",
                    "password          | NULL          | not-null",
                    "timeZone          | ''            | not-blank",
                })
        void shouldReportFieldCode_whenFieldBreaksRule(String field, String value, String code) {
            assertValidationFailed(post("/api/v1/invite-acceptances", with(field, value)), field, code);
        }

        @Test
        void shouldReportSize_whenNameIsLongerThan100Characters() {
            assertValidationFailed(
                    post("/api/v1/invite-acceptances", with("name", "a".repeat(101))), "name", "size");
        }

        @Test
        void shouldReportPattern_whenNameHasLineBreak() {
            assertValidationFailed(
                    post("/api/v1/invite-acceptances", with("name", "Anna\nKowalska")), "name", "pattern");
        }

        @Test
        void shouldReportAssertTrue_whenAgeIsNotConfirmed() {
            assertValidationFailed(
                    post("/api/v1/invite-acceptances", with("atLeastSixteen", false)), "atLeastSixteen", "assert-true");
        }

        @Test
        void shouldReportAssertTrue_whenConsentIsNotGiven() {
            assertValidationFailed(
                    post("/api/v1/invite-acceptances", with("healthDataConsent", false)),
                    "healthDataConsent",
                    "assert-true");
        }

        @Test
        void shouldAnswerLinkInvalidAndSignInNobody_whenLinkDoesNotWork() {
            when(signUp.joinAsClient(any())).thenThrow(new InviteLinkInvalidException());

            var result = post("/api/v1/invite-acceptances", validJoin());

            assertLinkInvalid(result);
            assertThat(result.getRequest().getSession(false))
                    .satisfiesAnyOf(
                            session -> assertThat(session).isNull(),
                            session -> assertThat(session.getAttribute(
                                            HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY))
                                    .isNull());
        }

        @Test
        void shouldAnswerLinkInvalid_whenAnotherJoinUsedTheInviteFirst() {
            when(signUp.joinAsClient(any())).thenThrow(new ObjectOptimisticLockingFailureException("Invite", "id"));

            assertLinkInvalid(post("/api/v1/invite-acceptances", validJoin()));
        }

        @Test
        void shouldAnswerLinkInvalid_whenAnotherJoinTookTheEmailFirst() {
            when(signUp.joinAsClient(any())).thenThrow(violation("uq_accounts_email"));

            assertLinkInvalid(post("/api/v1/invite-acceptances", validJoin()));
        }

        @Test
        void shouldAnswerInternalErrorWithoutDetails_whenAnyOtherConstraintIsViolated() throws Exception {
            when(signUp.joinAsClient(any())).thenThrow(violation("ck_accounts_client_trainer"));

            var result = post("/api/v1/invite-acceptances", validJoin());

            assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
            assertThat(result.getResponse().getContentAsString())
                    .doesNotContain("ck_accounts_client_trainer", "Exception", "insert");
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.post()
                    .uri("/api/v1/invite-acceptances")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(JSON.writeValueAsString(validJoin()))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
            verify(signUp, never()).joinAsClient(any());
        }

        private static DataIntegrityViolationException violation(String constraint) {
            var cause = new ConstraintViolationException(
                    "could not execute statement", new SQLException("insert into accounts"), constraint);
            return new DataIntegrityViolationException("could not execute statement", cause);
        }

        private void assertValidationFailed(MvcTestResult result, String field, String code) {
            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .contains(Map.of("field", field, "code", code));
            verify(signUp, never()).joinAsClient(any());
        }
    }
}
