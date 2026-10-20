package com.kinetiq;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Whole application against a real database, checking the security defaults. */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class KinetiqBackendApplicationIT {

    @Autowired
    private MockMvcTester mvc;

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
    void shouldHideExerciseLibrary_whenCallerIsAnonymous() {
        var result = mvc.get().uri("/api/v1/exercises").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
        assertThat(result).bodyJson().doesNotHavePath("$.exercises");
    }

    @Test
    @WithMockUser
    void shouldListStarterExercises_whenCallerIsSignedIn() {
        assertThat(mvc.get().uri("/api/v1/exercises"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.total")
                .isEqualTo(40);
    }

    @Test
    void shouldRejectStateChangingRequest_whenCsrfTokenIsMissing() {
        assertThat(mvc.post().uri("/api/v1/anything")).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldAcceptSignUp_whenAnonymousCallerSendsCsrfTokenFromCookie() {
        Cookie token = mvc.get().uri("/api/v1/csrf").exchange().getResponse().getCookie("XSRF-TOKEN");
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
