package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.output.AccountSessions;
import lombok.RequiredArgsConstructor;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JdbcAccountSessions implements AccountSessions {

    private final FindByIndexNameSessionRepository<?> sessions;

    // a session's principal name is the account id, set at sign-in
    @Override
    public void endAll(AccountId id) {
        sessions.findByPrincipalName(id.value().toString()).keySet().forEach(sessions::deleteById);
    }
}
