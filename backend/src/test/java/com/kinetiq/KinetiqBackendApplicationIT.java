package com.kinetiq;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Starts the whole application against a real database and checks the security defaults. */
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
    void shouldRejectStateChangingRequest_whenCsrfTokenIsMissing() {
        assertThat(mvc.post().uri("/api/v1/anything")).hasStatus(HttpStatus.FORBIDDEN);
    }
}
