package com.kinetiq.common.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

// no controller on purpose: only static content and the security rules are under test
@WebMvcTest(controllers = SpaFallbackTest.class)
@Import({SecurityConfig.class, WebConfig.class})
class SpaFallbackTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void shouldServeIndex_whenClientRouteRequested() {
        assertThat(mvc.get().uri("/trainer/plans"))
                .hasStatusOk()
                .bodyText()
                .contains("spa");
    }

    @Test
    void shouldReturnNotFound_whenAssetIsMissing() {
        assertThat(mvc.get().uri("/assets/missing.js")).hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturnUnauthorized_whenApiPathRequestedAnonymously() {
        assertThat(mvc.get().uri("/api/v1/nope")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void shouldReturnProblem_whenApiPathUnknown() {
        assertThat(mvc.get().uri("/api/v1/nope").with(user("someone").roles("CLIENT")))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
    }
}
