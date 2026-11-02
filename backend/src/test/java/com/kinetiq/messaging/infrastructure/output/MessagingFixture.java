package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Inserts accounts with SQL, as the migrations define them; messaging knows no class of the account feature. */
record MessagingFixture(JdbcTemplate jdbc) {

    AccountId trainer(String name) {
        return account(name, "TRAINER", null);
    }

    AccountId client(String name, AccountId trainer) {
        return account(name, "CLIENT", trainer.value());
    }

    private AccountId account(String name, String role, UUID trainer) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone, email_confirmed,
                                      health_data_consent_at, created_at, updated_at, version)
                values (?, ?, ?, 'hash', ?, ?, 'Europe/Warsaw', true,
                        case when ? = 'CLIENT' then now() end, now(), now(), 0)
                """,
                id,
                name,
                id + "@example.com",
                role,
                trainer,
                role);
        return new AccountId(id);
    }

    UUID conversation(AccountId client, AccountId trainer) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                """
                insert into conversations (id, client_id, trainer_id, created_at, updated_at, version)
                values (?, ?, ?, now(), now(), 0)
                """,
                id,
                client.value(),
                trainer.value());
        return id;
    }

    UUID message(UUID conversation, AccountId sender, String text, Instant receivedAt) {
        UUID id = UUID.randomUUID();
        message(id, conversation, sender, text, receivedAt);
        return id;
    }

    void message(UUID id, UUID conversation, AccountId sender, String text, Instant receivedAt) {
        jdbc.update(
                """
                insert into messages (id, conversation_id, sender_id, text, received_at, created_at, updated_at, version)
                values (?, ?, ?, ?, ?::timestamptz, now(), now(), 0)
                """,
                id,
                conversation,
                sender.value(),
                text,
                receivedAt.toString());
    }

    void read(UUID conversation, AccountId reader, Instant at) {
        jdbc.update(
                "insert into conversation_reads (conversation_id, account_id, last_read_at) values (?, ?, ?::timestamptz)",
                conversation,
                reader.value(),
                at.toString());
    }
}
