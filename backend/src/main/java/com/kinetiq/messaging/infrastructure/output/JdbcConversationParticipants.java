package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationPair;
import com.kinetiq.messaging.domain.output.ConversationParticipants;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// reads the link from the accounts, and no class of that feature
@Repository
@RequiredArgsConstructor
class JdbcConversationParticipants implements ConversationParticipants {

    private final JdbcTemplate jdbc;

    @Override
    public Optional<ConversationPair> pairOf(AccountId client) {
        return jdbc
                .query(
                        "select trainer_id from accounts where id = ? and role = 'CLIENT' and trainer_id is not null",
                        (rows, rowNumber) -> new ConversationPair(
                                client, new AccountId(rows.getObject("trainer_id", UUID.class))),
                        client.value())
                .stream()
                .findFirst();
    }
}
