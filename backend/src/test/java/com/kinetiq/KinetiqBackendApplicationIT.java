package com.kinetiq;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.model.Email;
import jakarta.servlet.http.Cookie;
import java.time.ZoneId;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Whole application against a real database, checking the security defaults. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class KinetiqBackendApplicationIT {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private JdbcIndexedSessionRepository sessions;

    @Autowired
    private AccountRepository accounts;

    @Autowired
    private PasswordHasher hasher;

    private Cookie csrfToken(Cookie... cookies) {
        var request = mvc.get().uri("/api/v1/csrf");
        if (cookies.length > 0) {
            request.cookie(cookies);
        }
        return request.exchange().getResponse().getCookie("XSRF-TOKEN");
    }

    private long storedSignIns() {
        return jdbc.sql("select count(*) from spring_session").query(Long.class).single();
    }

    @Test
    void shouldReportHealthUp_whenCallerIsAnonymous() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void shouldRejectRequest_whenCallerIsAnonymous() {
        assertThat(mvc.get().uri("/api/v1/anything")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldStoreSessionInDatabase_whenSessionIsSaved() {
        long before = storedSignIns();

        sessions.save(sessions.createSession());

        assertThat(storedSignIns()).isEqualTo(before + 1);
    }

    @Test
    void shouldStoreNoSession_whenAnonymousCallerIsRefused() {
        long before = storedSignIns();

        var result = mvc.get().uri("/api/v1/anything").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result.getResponse().getCookie("SESSION")).isNull();
        assertThat(storedSignIns()).isEqualTo(before);
    }

    @Test
    void shouldHideExerciseLibrary_whenCallerIsAnonymous() {
        var result = mvc.get().uri("/api/v1/exercises").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        assertThat(result).bodyJson().doesNotHavePath("$.exercises");
    }

    @Test
    @WithMockUser(roles = "TRAINER")
    void shouldListStarterExercises_whenCallerIsSignedIn() {
        assertThat(mvc.get().uri("/api/v1/exercises"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.total")
                .isEqualTo(40);
    }

    @Test
    @WithMockUser(roles = "CLIENT")
    void shouldRefuseExerciseLibrary_whenCallerIsClient() {
        var result = mvc.get().uri("/api/v1/exercises").exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-allowed");
        assertThat(result).bodyJson().doesNotHavePath("$.exercises");
    }

    @Test
    void shouldStoreSignInUntilSignOut_whenTrainerSignsIn() {
        AccountId id = AccountId.random();
        accounts.save(Account.restore(
                id,
                "Integration Trainer",
                new Email("it.trainer@example.com"),
                hasher.hash(new RawPassword("Kinetiq-2026")),
                Role.TRAINER,
                ZoneId.of("Europe/Warsaw"),
                true,
                null,
                null));
        Cookie token = csrfToken();

        var signedIn = mvc.post()
                .uri("/api/v1/sign-ins")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"IT.Trainer@example.com\",\"password\":\"Kinetiq-2026\"}")
                .exchange();

        assertThat(signedIn).hasStatusOk();
        Cookie session = signedIn.getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();
        // the token known before sign-in is cleared, so the next state-changing request needs a new one
        assertThat(signedIn.getResponse().getCookie("XSRF-TOKEN").getMaxAge()).isZero();
        assertThat(jdbc.sql("select count(*) from spring_session where principal_name = ?")
                        .param(id.value().toString())
                        .query(Long.class)
                        .single())
                .isOne();
        assertThat(mvc.get().uri("/api/v1/exercises").cookie(session)).hasStatusOk();
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(session))
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(id.value().toString());

        Cookie newToken = csrfToken(session);
        var signedOut = mvc.delete()
                .uri("/api/v1/sign-ins/current")
                .cookie(session, newToken)
                .header("X-XSRF-TOKEN", newToken.getValue())
                .exchange();

        assertThat(signedOut).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(session)).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReplaceEarlierSignIn_whenSomeoneSignsInOverIt() {
        saveAccount("it.first@example.com", Role.TRAINER);
        AccountId client = saveAccount("it.second@example.com", Role.CLIENT);
        Cookie trainer = signIn("it.first@example.com").getResponse().getCookie("SESSION");

        Cookie session = signIn("it.second@example.com", trainer).getResponse().getCookie("SESSION");

        assertThat(session.getValue()).isNotEqualTo(trainer.getValue());
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(trainer)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(session))
                .bodyJson()
                .extractingPath("$.id")
                .isEqualTo(client.value().toString());
        assertThat(mvc.get().uri("/api/v1/exercises").cookie(session)).hasStatus(HttpStatus.FORBIDDEN);
    }

    private AccountId saveAccount(String email, Role role) {
        AccountId id = AccountId.random();
        accounts.save(Account.restore(
                id,
                "Integration Account",
                new Email(email),
                hasher.hash(new RawPassword("Kinetiq-2026")),
                role,
                ZoneId.of("Europe/Warsaw"),
                true,
                null,
                null));
        return id;
    }

    private MvcTestResult signIn(String email, Cookie... cookies) {
        Cookie token = csrfToken(cookies);
        return mvc.post()
                .uri("/api/v1/sign-ins")
                .cookie(Stream.concat(Stream.of(cookies), Stream.of(token)).toArray(Cookie[]::new))
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"password\":\"Kinetiq-2026\"}".formatted(email))
                .exchange();
    }

    @Test
    void shouldRejectStateChangingRequest_whenCsrfTokenIsMissing() {
        assertThat(mvc.post().uri("/api/v1/anything")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAcceptSignUp_whenAnonymousCallerSendsCsrfTokenFromCookie() {
        Cookie token = csrfToken();
        assertThat(token).isNotNull();

        var result = mvc.post()
                .uri("/api/v1/accounts")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                        """
                        {"name":"Marek Nowak","email":"marek@example.com","password":"Kinetiq-2026",
                         "adult":true,"timeZone":"Europe/Warsaw"}
                        """)
                .exchange();

        assertThat(result).hasStatus(HttpStatus.ACCEPTED);
    }
}
