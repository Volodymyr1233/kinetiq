package com.kinetiq.common.infrastructure.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    // only reachable in development: springdoc is a developmentOnly dependency
    private static final String[] API_DOCS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) {
        return http.authorizeHttpRequests(requests -> requests
                        // without this the container's error dispatch is rejected and every error becomes 401
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/actuator/health")
                        .permitAll()
                        .requestMatchers(API_DOCS)
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .csrf(CsrfConfigurer::spa)
                // the CSRF filter uses this access-denied handler too, so a missing token also gets a code
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                                writeProblem(response, HttpStatus.UNAUTHORIZED, "request.unauthenticated"))
                        .accessDeniedHandler((request, response, exception) ->
                                writeProblem(response, HttpStatus.FORBIDDEN, "request.forbidden")))
                .build();
    }

    // these refusals happen before Spring MVC, so GlobalExceptionHandler never sees them
    private static void writeProblem(HttpServletResponse response, HttpStatus status, String code)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter()
                .write("{\"type\":\"about:blank\",\"title\":\"%s\",\"status\":%d,\"code\":\"%s\"}"
                        .formatted(status.getReasonPhrase(), status.value(), code));
    }
}
