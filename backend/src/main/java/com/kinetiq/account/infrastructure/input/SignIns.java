package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

/**
 * Signs a person in on this browser: a new session id and CSRF token replace the earlier ones, and the account id
 * becomes the principal.
 */
@Component
@RequiredArgsConstructor
class SignIns {

    private final SecurityContextHolderStrategy contexts = SecurityContextHolder.getContextHolderStrategy();

    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contextRepository;

    void start(Account account, HttpServletRequest request, HttpServletResponse response) {
        // the account id, never the email, so the session table holds no personal data
        Authentication authentication = UsernamePasswordAuthenticationToken.authenticated(
                account.getId().value().toString(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + account.getRole().name())));
        sessionStrategy.onAuthentication(authentication, request, response);
        SecurityContext context = contexts.createEmptyContext();
        context.setAuthentication(authentication);
        contexts.setContext(context);
        contextRepository.saveContext(context, request, response);
    }
}
