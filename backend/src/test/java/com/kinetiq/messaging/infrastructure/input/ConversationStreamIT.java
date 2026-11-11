package com.kinetiq.messaging.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.model.Email;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** The event stream of the whole application: the session cookie, security, a real commit and the stream's end. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "kinetiq.plan-drafting.api-key=")
@AutoConfigureMockMvc
class ConversationStreamIT {

    private static final String EVENTS = "/api/v1/conversations/events";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordHasher hasher;

    @Autowired
    private FindByIndexNameSessionRepository<?> sessions;

    @Autowired
    private ConversationStreams streams;

    /** A client, their trainer and the session cookie of each. */
    private record Pair(AccountId client, Cookie clientSignIn, AccountId trainer, Cookie trainerSignIn) {}

    private AccountId saveAccount(String email, Role role, AccountId trainer) {
        AccountId id = AccountId.random();
        accounts.save(Account.restore(
                id,
                "Stream Account",
                new Email(email),
                hasher.hash(new RawPassword("Kinetiq-2026")),
                role,
                trainer,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null,
                trainer == null ? null : Instant.now()));
        return id;
    }

    private Cookie csrfToken(Cookie... cookies) {
        var request = mvc.get().uri("/api/v1/csrf");
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return request.exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    private Cookie signIn(String email) {
        Cookie token = csrfToken();
        return mvc.post()
                .uri("/api/v1/sign-ins")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Kinetiq-2026\"}".formatted(email))
                .exchange()
                .getResponse()
                .getCookie("SESSION");
    }

    private Pair pair() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        String trainerEmail = "stream.trainer." + suffix + "@example.com";
        String clientEmail = "stream.client." + suffix + "@example.com";
        AccountId trainer = saveAccount(trainerEmail, Role.TRAINER, null);
        AccountId client = saveAccount(clientEmail, Role.CLIENT, trainer);
        return new Pair(client, signIn(clientEmail), trainer, signIn(trainerEmail));
    }

    private MvcTestResult stream(Cookie signIn) {
        MvcTestResult result = mvc.get().uri(EVENTS).cookie(signIn).asyncExchange();
        assertThat(result.getMvcResult().getRequest().isAsyncStarted()).isTrue();
        return result;
    }

    private static String received(MvcTestResult stream) throws Exception {
        return stream.getResponse().getContentAsString();
    }

    private void write(Cookie signIn, AccountId client, String text) {
        Cookie token = csrfToken(signIn);
        var sent = mvc.post()
                .uri("/api/v1/conversations/{clientId}/messages", client.value())
                .cookie(signIn, token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"id\":\"%s\",\"text\":\"%s\"}".formatted(UUID.randomUUID(), text))
                .exchange();
        assertThat(sent).hasStatus(HttpStatus.CREATED);
    }

    @Test
    void shouldRefuseTheStream_whenNobodyIsSignedIn() {
        var result = mvc.get().uri(EVENTS).exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
    }

    @Test
    void shouldTellTheClient_whenTheirTrainerWrites() throws Exception {
        Pair pair = pair();
        Pair other = pair();
        MvcTestResult client = stream(pair.clientSignIn());
        MvcTestResult stranger = stream(other.clientSignIn());

        write(pair.trainerSignIn(), pair.client(), "Nice depth on set 3");

        String message = "event:message\ndata:" + pair.client().value() + "\n\n";
        await().until(() -> received(client).contains(message));
        assertThat(client.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(received(client)).doesNotContain("Nice depth");
        assertThat(received(stranger)).startsWith("event:ping").doesNotContain("event:message");
    }

    @Test
    void shouldTellOnlyTheReader_whenAConversationIsMarkedRead() throws Exception {
        Pair pair = pair();
        write(pair.clientSignIn(), pair.client(), "Can I swap leg curls?");
        MvcTestResult trainer = stream(pair.trainerSignIn());
        MvcTestResult client = stream(pair.clientSignIn());
        Cookie token = csrfToken(pair.trainerSignIn());

        var read = mvc.put()
                .uri("/api/v1/conversations/{clientId}/read", pair.client().value())
                .cookie(pair.trainerSignIn(), token)
                .header("X-XSRF-TOKEN", token.getValue())
                .exchange();

        assertThat(read).hasStatus(HttpStatus.NO_CONTENT);
        await().until(() -> received(trainer).contains("event:read\ndata:" + pair.client().value()));
        assertThat(received(client)).doesNotContain("event:read");
    }

    @Test
    void shouldEndTheStream_whenItsSignInHasEnded() throws Exception {
        Pair pair = pair();
        MvcTestResult client = stream(pair.clientSignIn());
        sessions.findByPrincipalName(pair.client().value().toString()).keySet().forEach(sessions::deleteById);

        streams.heartbeat();
        write(pair.trainerSignIn(), pair.client(), "Still there?");

        // the stream's request finished: its async result is set
        assertThat(client.getMvcResult().getAsyncResult(5_000)).isNull();
        assertThat(received(client)).doesNotContain("event:message");
        assertThat(mvc.get().uri(EVENTS).cookie(pair.clientSignIn())).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
