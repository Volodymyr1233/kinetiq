package com.kinetiq.common.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.kinetiq.common.domain.exception.DomainException;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.ThrowingController.class)
@Import({GlobalExceptionHandlerTest.ThrowingController.class, SecurityConfig.class})
@WithMockUser
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void shouldHideInternals_whenExceptionIsUnexpected() {
        var result = mvc.get().uri("/test/unexpected").exchange();

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyText().doesNotContain("secret", "IllegalStateException");
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
        assertThat(result).bodyJson().extractingPath("$.correlationId").asString().isNotBlank();
    }

    @Test
    void shouldUseItsCode_whenDomainRuleIsBroken() {
        var result = mvc.get().uri("/test/domain").exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyText().doesNotContain("secret");
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("test.broken");
    }

    @Test
    void shouldAnswerInvalidWithoutDetail_whenJsonIsMalformed() {
        var result = mvc.post()
                .uri("/test/body")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.invalid");
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
    }

    @Test
    void shouldAnswerUnsupportedMediaType_whenBodyIsNotJson() {
        var result = mvc.post()
                .uri("/test/body")
                .with(csrf())
                .contentType(MediaType.TEXT_PLAIN)
                .content("name")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unsupported-media-type");
    }

    @Test
    void shouldAnswerMethodNotAllowed_whenMethodIsWrong() {
        var result = mvc.delete().uri("/test/body").with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.method-not-allowed");
    }

    @Test
    void shouldAnswerNotFound_whenEndpointDoesNotExist() {
        var result = mvc.get().uri("/test/missing").exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.not-found");
    }

    @Test
    void shouldListFieldCodes_whenBodyBreaksRules() {
        var result = mvc.post()
                .uri("/test/body")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"nickname\":\"far too long\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
        assertThat(result)
                .bodyJson()
                .extractingPath("$.errors")
                .asArray()
                .containsExactlyInAnyOrder(
                        Map.of("field", "name", "code", "not-blank"), Map.of("field", "nickname", "code", "size"));
    }

    @Test
    void shouldAnswerForbiddenWithCode_whenCsrfTokenIsMissing() {
        var result = mvc.post()
                .uri("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"a\"}")
                .exchange();

        assertThat(result).hasStatus(HttpStatus.FORBIDDEN).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.forbidden");
    }

    @Test
    @WithAnonymousUser
    void shouldAnswerUnauthenticatedWithCode_whenCallerIsAnonymous() {
        var result = mvc.get().uri("/test/domain").exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED).hasContentType(MediaType.APPLICATION_PROBLEM_JSON);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("secret table users");
        }

        @GetMapping("/test/domain")
        void domain() {
            throw new DomainException("secret rule") {
                @Override
                public String code() {
                    return "test.broken";
                }
            };
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody TestRequest request) {}

        record TestRequest(@NotBlank String name, @Size(max = 5) String nickname) {}
    }
}
