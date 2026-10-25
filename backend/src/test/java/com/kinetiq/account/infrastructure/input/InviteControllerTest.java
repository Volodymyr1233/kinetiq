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

import com.kinetiq.account.domain.exception.InviteAccountExistsException;
import com.kinetiq.account.domain.exception.InviteNotFoundException;
import com.kinetiq.account.domain.input.CreateInviteCommand;
import com.kinetiq.account.domain.input.CreatedInvite;
import com.kinetiq.account.domain.input.ManageInvitesUseCase;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.account.infrastructure.input.RequestLimiter.Kind;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(InviteController.class)
@Import({SecurityConfig.class, WebConfig.class, InviteWebMapperImpl.class})
class InviteControllerTest {

    private static final AccountId TRAINER_ID =
            new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user("00000000-0000-4000-8000-000000000003").roles("CLIENT");
    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final String LINK = "http://localhost:5173/join#token=abc";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ManageInvitesUseCase invites;

    @MockitoBean
    private RequestLimiter limiter;

    private final Invite invite = Invite.send(TRAINER_ID, new Email("julia@example.com"), NOW).invite();

    @BeforeEach
    void allowRequests() {
        when(limiter.tryAcquire(any(), anyString())).thenReturn(true);
    }

    private MvcTestResult create(String email, RequestPostProcessor user) {
        return mvc.post()
                .uri("/api/v1/invites")
                .with(csrf())
                .with(user)
                .header("Accept-Language", "pl")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email))
                .exchange();
    }

    @Nested
    class Create {

        @Test
        void shouldAnswerInviteAndLink_whenEmailIsValid() {
            when(invites.create(any())).thenReturn(new CreatedInvite(invite, LINK));

            var result = create(" Julia@Example.com ", TRAINER);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.link").isEqualTo(LINK);
            assertThat(result).bodyJson().extractingPath("$.invite.id").isEqualTo(invite.getId().value().toString());
            assertThat(result).bodyJson().extractingPath("$.invite.email").isEqualTo("julia@example.com");
            assertThat(result).bodyJson().extractingPath("$.invite.expiresAt").isEqualTo("2026-11-01T10:00:00Z");
            assertThat(result).bodyText().doesNotContain("Hash", invite.getLinkTokenHash());
            verify(invites)
                    .create(new CreateInviteCommand(
                            TRAINER_ID, new Email("julia@example.com"), Locale.forLanguageTag("pl")));
        }

        @ParameterizedTest
        @CsvSource({"'', not-blank", "julia@, pattern", "julia example.com, pattern"})
        void shouldAnswerFieldError_whenEmailIsInvalid(String email, String code) {
            var result = create(email, TRAINER);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .contains(Map.of("field", "email", "code", code));
            verify(invites, never()).create(any());
        }

        @Test
        void shouldAnswerFieldError_whenEmailIsTooLong() {
            var result = create("a".repeat(243) + "@example.com", TRAINER);

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(result)
                    .bodyJson()
                    .extractingPath("$.errors")
                    .asArray()
                    .contains(Map.of("field", "email", "code", "size"));
        }

        @Test
        void shouldAnswerConflict_whenEmailHasAccount() {
            when(invites.create(any())).thenThrow(new InviteAccountExistsException());

            var result = create("ola@example.com", TRAINER);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invite.account-exists");
        }

        @Test
        void shouldAnswerTooManyWithoutInviting_whenLimitIsReached() {
            when(limiter.tryAcquire(Kind.INVITE, TRAINER_ID.value().toString())).thenReturn(false);

            var result = create("julia@example.com", TRAINER);

            assertThat(result).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invite.limit-reached");
            verify(invites, never()).create(any());
        }

        @Test
        void shouldRefuse_whenCallerIsClient() {
            var result = create("julia@example.com", CLIENT);

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
            verify(invites, never()).create(any());
        }

        @Test
        void shouldRefuse_whenCsrfTokenIsMissing() {
            var result = mvc.post()
                    .uri("/api/v1/invites")
                    .with(TRAINER)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"email\":\"julia@example.com\"}")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
        }
    }

    @Nested
    class ListOpen {

        @Test
        void shouldAnswerOpenInvites_whenTrainerAsks() {
            when(invites.listOpen(TRAINER_ID)).thenReturn(List.of(invite));

            var result = mvc.get().uri("/api/v1/invites").with(TRAINER).exchange();

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.invites[0].email").isEqualTo("julia@example.com");
            assertThat(result).bodyJson().extractingPath("$.invites[0].sentAt").isEqualTo("2026-10-25T10:00:00Z");
        }

        @Test
        void shouldAnswerUnauthenticated_whenNobodyIsSignedIn() {
            var result = mvc.get().uri("/api/v1/invites").exchange();

            assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        }

        @Test
        void shouldRefuse_whenCallerIsClient() {
            var result = mvc.get().uri("/api/v1/invites").with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
        }
    }

    @Nested
    class ReplaceLink {

        @Test
        void shouldAnswerNewLink_whenInviteIsOwnAndOpen() {
            when(invites.replaceLink(TRAINER_ID, invite.getId())).thenReturn(LINK);

            var result = mvc.post()
                    .uri("/api/v1/invites/{id}/links", invite.getId().value())
                    .with(csrf())
                    .with(TRAINER)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.link").isEqualTo(LINK);
        }

        @Test
        void shouldAnswerNotFound_whenInviteIsNotOwnOrNotOpen() {
            InviteId id = InviteId.random();
            when(invites.replaceLink(TRAINER_ID, id)).thenThrow(new InviteNotFoundException());

            var result = mvc.post()
                    .uri("/api/v1/invites/{id}/links", id.value())
                    .with(csrf())
                    .with(TRAINER)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invite.not-found");
        }
    }

    @Nested
    class Revoke {

        @Test
        void shouldAnswerNoContent_whenInviteIsOwnAndOpen() {
            var result = mvc.delete()
                    .uri("/api/v1/invites/{id}", invite.getId().value())
                    .with(csrf())
                    .with(TRAINER)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
            verify(invites).revoke(TRAINER_ID, invite.getId());
        }

        @Test
        void shouldAnswerNotFound_whenInviteIsNotOwnOrNotOpen() {
            InviteId id = InviteId.random();
            doThrow(new InviteNotFoundException()).when(invites).revoke(TRAINER_ID, id);

            var result = mvc.delete()
                    .uri("/api/v1/invites/{id}", id.value())
                    .with(csrf())
                    .with(TRAINER)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invite.not-found");
        }

        @Test
        void shouldRefuse_whenCallerIsClient() {
            var result = mvc.delete()
                    .uri("/api/v1/invites/{id}", invite.getId().value())
                    .with(csrf())
                    .with(CLIENT)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
            verify(invites, never()).revoke(any(), any());
        }
    }
}
