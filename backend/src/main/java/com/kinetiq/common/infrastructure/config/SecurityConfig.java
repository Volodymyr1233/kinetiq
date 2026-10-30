package com.kinetiq.common.infrastructure.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.LogoutConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.CompositeLogoutHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfLogoutHandler;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    // only reachable in development: springdoc is a developmentOnly dependency
    private static final String[] API_DOCS = {"/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"};

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, SecurityContextRepository contextRepository, CsrfTokenRepository csrfTokens) {
        return http.authorizeHttpRequests(requests -> requests
                        // without this the container's error dispatch is rejected and every error becomes 401
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers("/actuator/health")
                        .permitAll()
                        .requestMatchers(API_DOCS)
                        .permitAll()
                        // sign-up, joining, sign-in and password reset: the person has no account or sign-in yet
                        // (specs "Public endpoints")
                        .requestMatchers(HttpMethod.GET, "/api/v1/csrf")
                        .permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/accounts",
                                "/api/v1/email-confirmations",
                                "/api/v1/confirmation-links",
                                "/api/v1/password-reset-links",
                                "/api/v1/password-resets",
                                "/api/v1/sign-ins",
                                "/api/v1/invite-checks",
                                "/api/v1/invite-acceptances")
                        .permitAll()
                        .requestMatchers("/api/v1/exercises/**", "/api/v1/invites/**")
                        .hasRole("TRAINER")
                        .requestMatchers("/api/v1/quiz/**")
                        .hasRole("CLIENT")
                        .requestMatchers("/api/**")
                        .authenticated()
                        // the SPA: static files and client-side routes are public, anything else needs a sign-in
                        .requestMatchers(HttpMethod.GET, "/**")
                        .permitAll()
                        .anyRequest()
                        .authenticated())
                .securityContext(context -> context.securityContextRepository(contextRepository))
                // spa() first, so its request handler stays and only the repository is shared with sign-in
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokens))
                // sign-out is an endpoint of the account feature, so it appears in the API contract
                .logout(LogoutConfigurer::disable)
                // a saved request would store a session row for every anonymous call; the SPA keeps its own return path
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                // the CSRF filter uses this access-denied handler too, so a missing token also gets a code
                .exceptionHandling(errors -> errors.authenticationEntryPoint((request, response, exception) ->
                                writeProblem(response, HttpStatus.UNAUTHORIZED, "request.unauthenticated"))
                        .accessDeniedHandler((request, response, exception) -> writeProblem(
                                response,
                                HttpStatus.FORBIDDEN,
                                exception instanceof CsrfException ? "request.forbidden" : "request.not-allowed")))
                .build();
    }

    @Bean
    SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    CsrfTokenRepository csrfTokenRepository() {
        return CookieCsrfTokenRepository.withHttpOnlyFalse();
    }

    /** What a sign-in must do besides storing the context: a new session id and a new CSRF token. */
    @Bean
    SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokens) {
        return new CompositeSessionAuthenticationStrategy(List.of(
                new ChangeSessionIdAuthenticationStrategy(), new CsrfAuthenticationStrategy(csrfTokens)));
    }

    /** What a sign-out must do: end the stored session, clear the context and the CSRF token. */
    @Bean
    LogoutHandler logoutHandler(CsrfTokenRepository csrfTokens) {
        return new CompositeLogoutHandler(new SecurityContextLogoutHandler(), new CsrfLogoutHandler(csrfTokens));
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
