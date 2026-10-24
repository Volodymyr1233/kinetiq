package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.SignInCommand;
import com.kinetiq.account.domain.input.SignInUseCase;
import com.kinetiq.account.domain.model.Account;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/sign-ins")
@RequiredArgsConstructor
class SignInController {

    private final SecurityContextHolderStrategy contexts = SecurityContextHolder.getContextHolderStrategy();

    private final SignInUseCase signIn;
    private final AccountWebMapper mapper;
    private final SessionAuthenticationStrategy sessionStrategy;
    private final SecurityContextRepository contextRepository;
    private final LogoutHandler logoutHandler;

    /**
     * Signs in and answers who is signed in. A new session id and CSRF token replace the earlier ones, so an id known
     * before sign-in is useless after it.
     */
    @PostMapping
    CurrentAccountResponse signIn(
            @Valid @RequestBody SignInRequest body, HttpServletRequest request, HttpServletResponse response) {
        Account account = signIn.signIn(new SignInCommand(body.email(), body.password()));
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
        return mapper.toResponse(account);
    }

    /** Ends this browser's sign-in on the server; other devices stay signed in. */
    @DeleteMapping("/current")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void signOut(Authentication authentication, HttpServletRequest request, HttpServletResponse response) {
        logoutHandler.logout(request, response, authentication);
    }
}
