package com.kinetiq.common.infrastructure.input;

import com.kinetiq.common.domain.exception.DomainException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Turns every error into a problem detail with a stable code. No exception text ever reaches the client. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";

    @ExceptionHandler(DomainException.class)
    ResponseEntity<ProblemDetail> handleDomain(DomainException exception) {
        HttpStatus status = switch (exception.kind()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case TOO_MANY_ATTEMPTS -> HttpStatus.TOO_MANY_REQUESTS;
        };
        ProblemDetail problem = problem(status, exception.code());
        exception.properties().forEach(problem::setProperty);
        return ResponseEntity.status(status).body(problem);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ProblemDetail> handleUnexpected(Exception exception) {
        String correlationId = UUID.randomUUID().toString();
        log.error("unexpected failure, correlationId={}", correlationId, exception);
        ProblemDetail problem = problem(HttpStatus.INTERNAL_SERVER_ERROR, "internal.error");
        problem.setProperty("correlationId", correlationId);
        return ResponseEntity.internalServerError().body(problem);
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "validation.failed");
        List<Map<String, String>> errors = exception.getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), CODE, kebabCase(error.getCode())))
                .toList();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, HttpStatus.BAD_REQUEST, request);
    }

    // Spring's own exceptions keep their status but lose their detail, which may echo request internals
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception exception,
            @Nullable Object body,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(exception, body, headers, status, request);
        if (response != null && response.getBody() instanceof ProblemDetail problem) {
            problem.setDetail(null);
            if (problem.getProperties() == null || !problem.getProperties().containsKey(CODE)) {
                problem.setProperty(CODE, codeFor(status));
            }
        }
        return response;
    }

    private static ProblemDetail problem(HttpStatus status, String code) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setProperty(CODE, code);
        return problem;
    }

    private static String codeFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 404 -> "request.not-found";
            case 405 -> "request.method-not-allowed";
            case 415 -> "request.unsupported-media-type";
            default -> status.is4xxClientError() ? "request.invalid" : "internal.error";
        };
    }

    /** Turns a constraint name into its error code, e.g. {@code NotBlank} into {@code not-blank}. */
    private static String kebabCase(@Nullable String constraint) {
        return constraint == null ? "invalid" : constraint.replaceAll("([a-z])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
    }
}
