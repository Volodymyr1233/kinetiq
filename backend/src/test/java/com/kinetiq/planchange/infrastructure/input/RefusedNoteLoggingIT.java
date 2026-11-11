package com.kinetiq.planchange.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * What a person typed into a note never reaches the log, even when the request is refused: the whole application's
 * logging, not the controller, is what is checked.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(properties = "kinetiq.plan-drafting.api-key=")
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class RefusedNoteLoggingIT {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void shouldKeepATooLongNoteOutOfTheLog_whenTheRequestIsRefused(CapturedOutput output) {
        String note = "health detail ".repeat(60);
        UUID trainer = UUID.randomUUID();

        var result = mvc.post()
                .uri("/api/v1/plan-changes/" + UUID.randomUUID() + "/declination")
                .with(user(trainer.toString()).roles("TRAINER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\",\"note\":\"" + note + "\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("note");
        assertThat(output.getAll()).doesNotContain("health detail");
    }

    @Test
    void shouldKeepAnUnreadableBodyOutOfTheLog() {
        // the body is sent as text a client never sends; the refusal must say nothing of it either
        var result = mvc.post()
                .uri("/api/v1/plan-changes/" + UUID.randomUUID() + "/declination")
                .with(user(UUID.randomUUID().toString()).roles("TRAINER"))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"reason\":\"OTHER\",\"note\": health detail")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyText().doesNotContain("health detail");
    }
}
