package com.kinetiq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jayway.jsonpath.JsonPath;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.GeneratedPlan.GeneratedEntry;
import com.kinetiq.plan.domain.model.GeneratedPlan.GeneratedSession;
import com.kinetiq.plan.domain.output.PlanGenerator;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.Duration;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Whole application against a real database, checking the security defaults. The empty key proves it starts without
 * one, even where the environment has it, and keeps every test away from the real service.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "kinetiq.plan-drafting.api-key=")
@AutoConfigureMockMvc
class KinetiqBackendApplicationIT {

    private static final String STARTER_PLANK = "00000000-0000-4000-9100-000000000026";

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

    @MockitoBean
    private PlanGenerator generator;

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

    // the library logs a whole prompt when a response has no choices
    @Test
    void shouldKeepSpringAiOutOfTheLog() {
        assertThat(LoggerFactory.getLogger("org.springframework.ai.openai.OpenAiChatModel").isErrorEnabled())
                .isFalse();
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
    void shouldRefuseQuiz_whenCallerIsAnonymous() {
        var result = mvc.get().uri("/api/v1/quiz").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
    }

    @Test
    @WithMockUser(username = "00000000-0000-4000-8000-0000000000aa", roles = "TRAINER")
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
                null,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
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

    // a client gets a trainer of their own, as the database requires
    private AccountId saveAccount(String email, Role role) {
        AccountId trainerId = role == Role.CLIENT ? saveAccount("coach." + email, Role.TRAINER) : null;
        AccountId id = AccountId.random();
        accounts.save(Account.restore(
                id,
                "Integration Account",
                new Email(email),
                hasher.hash(new RawPassword("Kinetiq-2026")),
                role,
                trainerId,
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null,
                trainerId == null ? null : Instant.now()));
        return id;
    }

    @Test
    void shouldEndEverySignInAndAcceptNewPassword_whenPasswordIsReset() {
        AccountId id = saveAccount("it.reset@example.com", Role.TRAINER);
        Cookie laptop = signIn("it.reset@example.com").getResponse().getCookie("SESSION");
        Cookie phone = signIn("it.reset@example.com").getResponse().getCookie("SESSION");
        SecretToken link = SecretToken.generate();
        Account account = accounts.findById(id).orElseThrow();
        account.issuePasswordReset(link, Instant.now());
        accounts.save(account);
        Cookie token = csrfToken();

        var reset = mvc.post()
                .uri("/api/v1/password-resets")
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\",\"password\":\"New-pass-2026\"}".formatted(link.value()))
                .exchange();

        assertThat(reset).hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(laptop)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(phone)).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(signIn("it.reset@example.com")).hasStatus(HttpStatus.UNAUTHORIZED);
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
    void shouldManageOwnInvitesOnly_whenTrainersAreSignedIn() throws Exception {
        saveAccount("it.inviter@example.com", Role.TRAINER);
        saveAccount("it.other@example.com", Role.TRAINER);
        Cookie trainer = signIn("it.inviter@example.com").getResponse().getCookie("SESSION");
        Cookie other = signIn("it.other@example.com").getResponse().getCookie("SESSION");

        var created = send(trainer, mvc.post().uri("/api/v1/invites"), "{\"email\":\"it.invited@example.com\"}");
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.link").asString().contains("/join#token=");
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.invite.id");

        assertThat(mvc.get().uri("/api/v1/invites").cookie(trainer))
                .bodyJson()
                .extractingPath("$.invites[*].id")
                .asArray()
                .containsExactly(id);
        assertThat(send(trainer, mvc.post().uri("/api/v1/invites/{id}/links", id), null))
                .hasStatus(HttpStatus.CREATED);
        assertThat(send(other, mvc.delete().uri("/api/v1/invites/{id}", id), null))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("invite.not-found");
        assertThat(send(trainer, mvc.delete().uri("/api/v1/invites/{id}", id), null))
                .hasStatus(HttpStatus.NO_CONTENT);
        assertThat(mvc.get().uri("/api/v1/invites").cookie(trainer))
                .bodyJson()
                .extractingPath("$.invites")
                .asArray()
                .isEmpty();
    }

    @Test
    void shouldJoinAsClientOfInvitingTrainerOnly_whenTwoTrainersInvitedTheEmail() throws Exception {
        AccountId inviter = saveAccount("it.join.first@example.com", Role.TRAINER);
        saveAccount("it.join.second@example.com", Role.TRAINER);
        Cookie first = signIn("it.join.first@example.com").getResponse().getCookie("SESSION");
        Cookie second = signIn("it.join.second@example.com").getResponse().getCookie("SESSION");
        String firstToken = inviteToken(first, "it.joiner@example.com");
        String secondToken = inviteToken(second, "it.joiner@example.com");

        assertThat(anonymous("/api/v1/invite-checks", "{\"token\":\"%s\"}".formatted(firstToken)))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.email")
                .isEqualTo("it.joiner@example.com");
        String join =
                """
                {"token":"%s","name":"Anna Kowalska","password":"Kinetiq-2026","atLeastSixteen":true,
                 "healthDataConsent":true,"timeZone":"Europe/Warsaw"}
                """;
        var joined = anonymous("/api/v1/invite-acceptances", join.formatted(firstToken));

        assertThat(joined).hasStatus(HttpStatus.CREATED);
        Cookie client = joined.getResponse().getCookie("SESSION");
        assertThat(client).isNotNull();
        assertThat(mvc.get().uri("/api/v1/accounts/me").cookie(client))
                .bodyJson()
                .extractingPath("$.role")
                .isEqualTo("CLIENT");
        var row = jdbc.sql("select trainer_id, health_data_consent_at from accounts where email = ?")
                .param("it.joiner@example.com")
                .query()
                .singleRow();
        assertThat(row.get("trainer_id")).isEqualTo(inviter.value());
        assertThat(row.get("health_data_consent_at")).isNotNull();
        assertThat(anonymous("/api/v1/invite-acceptances", join.formatted(firstToken)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("invite.link-invalid");
        assertThat(anonymous("/api/v1/invite-checks", "{\"token\":\"%s\"}".formatted(secondToken)))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("invite.link-invalid");
    }

    private String inviteToken(Cookie trainer, String email) throws Exception {
        var created = send(trainer, mvc.post().uri("/api/v1/invites"), "{\"email\":\"%s\"}".formatted(email));
        String link = JsonPath.read(created.getResponse().getContentAsString(), "$.link");
        return link.substring(link.indexOf("#token=") + "#token=".length());
    }

    private MvcTestResult anonymous(String uri, String json) {
        Cookie token = csrfToken();
        return mvc.post()
                .uri(uri)
                .cookie(token)
                .header("X-XSRF-TOKEN", token.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json)
                .exchange();
    }

    @Test
    void shouldKeepOwnExercisesApart_whenTwoTrainersManageThem() throws Exception {
        saveAccount("it.coach.a@example.com", Role.TRAINER);
        saveAccount("it.coach.b@example.com", Role.TRAINER);
        Cookie trainerA = signIn("it.coach.a@example.com").getResponse().getCookie("SESSION");
        Cookie trainerB = signIn("it.coach.b@example.com").getResponse().getCookie("SESSION");
        String exercise =
                """
                {"name":"%s","category":"LEGS","measure":"REPS","equipment":"Dumbbell",
                 "sets":3,"amount":10,"restSeconds":90,"cues":""}
                """;

        var created = send(trainerA, mvc.post().uri("/api/v1/exercises"), exercise.formatted("Goblet squat"));
        assertThat(created).hasStatus(HttpStatus.CREATED);
        assertThat(created).bodyJson().extractingPath("$.starter").isEqualTo(false);
        String id = JsonPath.read(created.getResponse().getContentAsString(), "$.id");
        String edit = exercise.formatted("Goblet squat hold").replace("\"measure\":\"REPS\",", "");
        assertThat(send(trainerA, mvc.put().uri("/api/v1/exercises/{id}", id), edit))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.name")
                .isEqualTo("Goblet squat hold");

        assertThat(mvc.get().uri("/api/v1/exercises?name=goblet").cookie(trainerA))
                .bodyJson()
                .extractingPath("$.exercises[*].id")
                .asArray()
                .containsExactly(id);
        var listedToB = mvc.get().uri("/api/v1/exercises?name=goblet").cookie(trainerB).exchange();
        assertThat(listedToB).bodyJson().extractingPath("$.exercises").asArray().isEmpty();
        assertThat(listedToB).bodyJson().extractingPath("$.total").isEqualTo(40);

        assertThat(send(trainerB, mvc.put().uri("/api/v1/exercises/{id}", id), edit))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("exercise.not-found");
        assertThat(send(trainerA, mvc.put().uri("/api/v1/exercises/{id}", STARTER_PLANK), edit))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("exercise.not-found");
        assertThat(send(trainerA, mvc.delete().uri("/api/v1/exercises/{id}", id), null))
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(send(trainerB, mvc.post().uri("/api/v1/exercises"), exercise.formatted("Goblet squat hold")))
                .hasStatus(HttpStatus.CREATED);
        for (String starterName : new String[] {"plank", "Deska"}) {
            assertThat(send(trainerA, mvc.post().uri("/api/v1/exercises"), exercise.formatted(starterName)))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("exercise.name-taken");
        }
        assertThat(mvc.get().uri("/api/v1/exercises").cookie(trainerA))
                .bodyJson()
                .extractingPath("$.total")
                .isEqualTo(41);
    }

    // a state-changing request of a signed-in person, with a CSRF token fetched for that sign-in
    private MvcTestResult send(Cookie session, MockMvcTester.MockMvcRequestBuilder request, String json) {
        Cookie token = csrfToken(session);
        request.cookie(session, token).header("X-XSRF-TOKEN", token.getValue());
        if (json != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(json);
        }
        return request.exchange();
    }

    private static final String QUIZ =
            """
            {"goal":"LOSE_FAT","experience":"YEARS_1_TO_2","level":"INTERMEDIATE","age":30,"heightCm":178,
             "weightKg":80.0,"likes":["WEIGHTS"],"daysPerWeek":2,"notes":"Left knee"}
            """;

    private static GeneratedPlan validAnswer() {
        List<GeneratedEntry> entries = List.of("E1", "E2", "E3", "E4").stream()
                .map(key -> new GeneratedEntry(key, 3, 10, null, 7, 90))
                .toList();
        return new GeneratedPlan(
                List.of(
                        new GeneratedSession("Lower body", "TUESDAY", entries),
                        new GeneratedSession("Upper body", "FRIDAY", entries)));
    }

    private void assertQuizStatus(Cookie client, String status) {
        assertThat(mvc.get().uri("/api/v1/quiz").cookie(client))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo(status);
    }

    private AccountId trainerOf(AccountId client) {
        return accounts.findById(client).orElseThrow().getTrainerId().orElseThrow();
    }

    @Test
    void shouldAnswerAtOnceAndDraftInTheBackground_whenAQuizIsSubmitted() throws Exception {
        reset(generator);
        CountDownLatch release = new CountDownLatch(1);
        when(generator.generate(any())).thenAnswer(call -> {
            assertThat(release.await(30, TimeUnit.SECONDS)).isTrue();
            return validAnswer();
        });
        AccountId clientId = saveAccount("it.quiz.client@example.com", Role.CLIENT);
        Cookie client = signIn("it.quiz.client@example.com").getResponse().getCookie("SESSION");
        Cookie trainer = signIn("coach.it.quiz.client@example.com").getResponse().getCookie("SESSION");
        String planAddress = "/api/v1/clients/" + clientId.value() + "/plan";

        assertThat(send(client, mvc.post().uri("/api/v1/quiz"), QUIZ))
                .hasStatus(HttpStatus.CREATED)
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("DRAFTING_PLAN");

        assertQuizStatus(client, "DRAFTING_PLAN");
        assertThat(mvc.get().uri("/api/v1/clients/" + clientId.value()).cookie(trainer))
                .bodyJson()
                .extractingPath("$.planState")
                .isEqualTo("PLAN_DRAFTING");
        assertThat(mvc.get().uri(planAddress).cookie(trainer)).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send(
                        trainer,
                        mvc.put().uri(planAddress),
                        "{\"goal\":\"LOSE_FAT\",\"weeks\":12,\"sessions\":[]}"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("plan.drafting");

        release.countDown();
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertQuizStatus(client, "WAITING_FOR_PLAN"));

        var plan = mvc.get().uri(planAddress).cookie(trainer).exchange();
        assertThat(plan).hasStatusOk();
        assertThat(plan).bodyJson().extractingPath("$.automaticDraft").isEqualTo("BUILT");
        assertThat(plan).bodyJson().extractingPath("$.goal").isEqualTo("LOSE_FAT");
        assertThat(plan).bodyJson().extractingPath("$.weeks").isEqualTo(12);
        assertThat(plan).bodyJson().extractingPath("$.sessions[0].day").isEqualTo("TUESDAY");
        assertThat(plan).bodyJson().extractingPath("$.sessions[1].day").isEqualTo("FRIDAY");
        assertThat(mvc.get().uri("/api/v1/clients/" + clientId.value()).cookie(trainer))
                .bodyJson()
                .extractingPath("$.planState")
                .isEqualTo("PLAN_TO_REVIEW");
        assertThat(mvc.get().uri("/api/v1/plan").cookie(client))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("plan.not-found");
    }

    @Test
    void shouldStartNothing_whenASubmitIsRefused() {
        reset(generator);
        when(generator.generate(any())).thenReturn(validAnswer());
        AccountId clientId = saveAccount("it.quiz.twice@example.com", Role.CLIENT);
        Cookie client = signIn("it.quiz.twice@example.com").getResponse().getCookie("SESSION");

        assertThat(send(client, mvc.post().uri("/api/v1/quiz"), QUIZ.replace("\"age\":30", "\"age\":15")))
                .hasStatus(HttpStatus.BAD_REQUEST);
        verify(generator, never()).generate(any());
        assertQuizStatus(client, "NOT_SUBMITTED");

        assertThat(send(client, mvc.post().uri("/api/v1/quiz"), QUIZ)).hasStatus(HttpStatus.CREATED);
        Awaitility.await()
                .atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertQuizStatus(client, "WAITING_FOR_PLAN"));

        assertThat(send(client, mvc.post().uri("/api/v1/quiz"), QUIZ))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("quiz.already-submitted");
        assertThat(jdbc.sql("select count(*) from plan_draftings where client_id = ?")
                        .param(clientId.value())
                        .query(Long.class)
                        .single())
                .isZero();
        verify(generator, times(1)).generate(any());
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
