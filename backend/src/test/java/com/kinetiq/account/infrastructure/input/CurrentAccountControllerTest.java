package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.exception.AccountNotFoundException;
import com.kinetiq.account.domain.exception.NameNotChangeableException;
import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(CurrentAccountController.class)
@Import({SecurityConfig.class, WebConfig.class, AccountWebMapperImpl.class})
class CurrentAccountControllerTest {

    private static final AccountId ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final RequestPostProcessor CLIENT = user(ID.value().toString()).roles("CLIENT");

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private CurrentAccountUseCase currentAccount;

    private MvcTestResult putTimeZone(String body, RequestPostProcessor... user) {
        var request = mvc.put()
                .uri("/api/v1/accounts/me/time-zone")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        for (RequestPostProcessor processor : user) {
            request.with(processor);
        }
        return request.exchange();
    }

    private MvcTestResult patch(String body, RequestPostProcessor... user) {
        var request = mvc.patch()
                .uri("/api/v1/accounts/me")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        for (RequestPostProcessor processor : user) {
            request.with(processor);
        }
        return request.exchange();
    }

    private static Account client(AccountSettings settings) {
        return Account.restore(
                ID,
                "Anna Kowalska",
                new Email("anna@example.com"),
                "hash",
                Role.CLIENT,
                ZoneId.of("Europe/Warsaw"),
                settings,
                true,
                null,
                null);
    }

    @Nested
    class Find {

        @Test
        void shouldAnswerSignedInAccount_whenSignedIn() {
            when(currentAccount.find(ID)).thenReturn(client(AccountSettings.startingWith(Language.EN)));

            var result = mvc.get().uri("/api/v1/accounts/me").with(CLIENT).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(ID.value().toString());
            assertThat(result).bodyJson().extractingPath("$.name").isEqualTo("Anna Kowalska");
            assertThat(result).bodyJson().extractingPath("$.email").isEqualTo("anna@example.com");
            assertThat(result).bodyJson().extractingPath("$.role").isEqualTo("CLIENT");
            assertThat(result).bodyJson().extractingPath("$.timeZone").isEqualTo("Europe/Warsaw");
            assertThat(result).bodyJson().extractingPath("$.units").isEqualTo("KG");
            assertThat(result).bodyJson().extractingPath("$.language").isEqualTo("EN");
            assertThat(result).bodyJson().extractingPath("$.theme").isEqualTo("SYSTEM");
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = mvc.get().uri("/api/v1/accounts/me").exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        }

        @Test
        void shouldAnswerUnauthenticated_whenAccountNoLongerExists() {
            when(currentAccount.find(ID)).thenThrow(new AccountNotFoundException());

            var result = mvc.get().uri("/api/v1/accounts/me").with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        }
    }

    @Nested
    class ChangeTimeZone {

        @Test
        void shouldStoreZone_whenItIsARegion() {
            var result = putTimeZone("{\"timeZone\":\"Europe/London\"}", CLIENT);

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            verify(currentAccount).changeTimeZone(ID, ZoneId.of("Europe/London"));
        }

        @Test
        void shouldFailValidation_whenZoneIsUnknown() {
            var result = putTimeZone("{\"timeZone\":\"Etc/Unknown\"}", CLIENT);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
            verify(currentAccount, never()).changeTimeZone(any(), any());
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = putTimeZone("{\"timeZone\":\"Europe/London\"}");

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
            verify(currentAccount, never()).changeTimeZone(any(), any());
        }
    }

    @Nested
    class Update {

        @Test
        void shouldChangeOnlyTheSentValueAndAnswerAccount_whenValid() {
            when(currentAccount.update(ID, new UpdateAccountCommand(null, null, null, Theme.DARK)))
                    .thenReturn(client(new AccountSettings(WeightUnit.KG, Language.EN, Theme.DARK)));

            var result = patch("{\"theme\":\"DARK\"}", CLIENT);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.theme").isEqualTo("DARK");
            assertThat(result).bodyJson().extractingPath("$.units").isEqualTo("KG");
            verify(currentAccount).update(ID, new UpdateAccountCommand(null, null, null, Theme.DARK));
        }

        @ParameterizedTest
        @ValueSource(strings = {"units", "language", "theme"})
        void shouldFailValidationOnThatField_whenValueIsUnknown(String field) {
            var result = patch("{\"" + field + "\":\"XX\"}", CLIENT);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
            assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo(field);
            assertThat(result).bodyJson().extractingPath("$.errors[0].code").isEqualTo("pattern");
            verify(currentAccount, never()).update(any(), any());
        }

        @ParameterizedTest
        @ValueSource(strings = {"   ", "Anna\\nNowak"})
        void shouldFailValidationOnName_whenBlankOrHasControlCharacters(String name) {
            var result = patch("{\"name\":\"" + name + "\"}", CLIENT);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
            assertThat(result).bodyJson().extractingPath("$.errors[0].code").isEqualTo("pattern");
            verify(currentAccount, never()).update(any(), any());
        }

        @Test
        void shouldFailValidationOnName_when101Characters() {
            var result = patch("{\"name\":\"" + "a".repeat(101) + "\"}", CLIENT);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("name");
            assertThat(result).bodyJson().extractingPath("$.errors[0].code").isEqualTo("size");
            verify(currentAccount, never()).update(any(), any());
        }

        @Test
        void shouldRefuse_whenClientChangesName() {
            when(currentAccount.update(ID, new UpdateAccountCommand("Anna Nowak", null, null, null)))
                    .thenThrow(new NameNotChangeableException());

            var result = patch("{\"name\":\"Anna Nowak\"}", CLIENT);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("account.name-not-changeable");
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = patch("{\"theme\":\"DARK\"}");

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
            verify(currentAccount, never()).update(any(), any());
        }
    }
}
