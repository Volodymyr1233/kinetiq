package com.kinetiq.account.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.exception.TrainerHasClientsException;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Email;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

/**
 * Proves that deleting a client leaves nothing of them in any table, and changes nobody else's data (spec 167, FR-008,
 * FR-010, SC-001). A new table must be named below as a client's or not, so it cannot be forgotten.
 */
@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    AccountRepositoryImpl.class,
    AccountMapperImpl.class,
    InviteRepositoryImpl.class,
    InviteMapperImpl.class
})
@Sql("erasure-fixture.sql")
class AccountErasureIT {

    private static final AccountId MAREK = id("10000000-0000-4000-8000-000000000001");
    private static final AccountId EWA = id("10000000-0000-4000-8000-000000000002");
    private static final AccountId ZOFIA = id("10000000-0000-4000-8000-000000000003");
    private static final AccountId PIOTR = id("10000000-0000-4000-8000-000000000004");
    private static final Email ZOFIA_EMAIL = new Email("zofia.erasure@example.com");

    // tables in which the fixture gives the client a row, which the deletion must take away
    private static final Set<String> CLIENT_TABLES = Set.of(
            "accounts",
            "invites",
            "quiz_submissions",
            "quiz_submission_likes",
            "plans",
            "plan_sessions",
            "plan_session_entries",
            "plan_draftings",
            "session_logs",
            "session_log_entries",
            "set_logs",
            "check_ins",
            "weekly_checks",
            "plan_changes",
            "plan_change_targets",
            "plan_change_moves",
            "plan_change_figures",
            "plan_change_exercises",
            "conversations",
            "messages",
            "conversation_reads",
            "notifications");

    // tables that hold nothing of a client: a trainer's own data, sign-ins (ended separately) and Flyway's history
    private static final Set<String> OTHER_TABLES = Set.of(
            "exercises",
            "plan_build_requests",
            "spring_session",
            "spring_session_attributes",
            "flyway_schema_history");

    @Autowired
    private AccountRepositoryImpl accounts;

    @Autowired
    private InviteRepositoryImpl invites;

    @Autowired
    private JdbcTemplate jdbc;

    private static AccountId id(String value) {
        return new AccountId(UUID.fromString(value));
    }

    private void deleteClient(AccountId client, Email email) {
        invites.deleteByTrainerAndEmail(MAREK, email);
        accounts.delete(client);
    }

    private List<String> tables() {
        return jdbc.queryForList(
                "select table_name from information_schema.tables where table_schema = 'public' and table_type = 'BASE TABLE'",
                String.class);
    }

    private List<Map<String, Object>> columns(String... types) {
        return jdbc.queryForList(
                "select c.table_name, c.column_name from information_schema.columns c"
                        + " join information_schema.tables t on t.table_name = c.table_name and t.table_schema = c.table_schema"
                        + " where c.table_schema = 'public' and t.table_type = 'BASE TABLE' and c.data_type = any(?)",
                (Object) types);
    }

    private Map<String, Integer> rowCounts() {
        Map<String, Integer> counts = new HashMap<>();
        for (String table : tables()) {
            counts.put(table, jdbc.queryForObject("select count(*) from " + table, Integer.class));
        }
        return counts;
    }

    // rows in any table with any uuid column holding the id
    private int references(AccountId account) {
        int found = 0;
        for (Map<String, Object> column : columns("uuid")) {
            found += jdbc.queryForObject(
                    "select count(*) from %s where %s = ?".formatted(column.get("table_name"), column.get("column_name")),
                    Integer.class,
                    account.value());
        }
        return found;
    }

    // rows in any table with any text column containing the value
    private int mentions(String value) {
        int found = 0;
        for (Map<String, Object> column : columns("character varying", "character", "text")) {
            found += jdbc.queryForObject(
                    "select count(*) from %s where %s ilike ?".formatted(column.get("table_name"), column.get("column_name")),
                    Integer.class,
                    "%" + value + "%");
        }
        return found;
    }

    @Test
    void shouldKnowWhetherEveryTableHoldsClientData() {
        assertThat(tables())
                .as("a new table must be added to CLIENT_TABLES (with a fixture row) or to OTHER_TABLES")
                .allMatch(table -> CLIENT_TABLES.contains(table) || OTHER_TABLES.contains(table));
    }

    @Test
    void shouldLeaveNothingOfTheClient_andChangeNoOneElsesData() {
        Map<String, Integer> before = rowCounts();
        int othersBefore = references(EWA) + references(PIOTR);
        assertThat(references(ZOFIA)).isPositive();

        deleteClient(ZOFIA, ZOFIA_EMAIL);

        Map<String, Integer> after = rowCounts();
        assertThat(CLIENT_TABLES)
                .as("every table in which the client had a row lost it")
                .allMatch(table -> after.get(table) < before.get(table));
        assertThat(references(ZOFIA)).isZero();
        assertThat(mentions(ZOFIA_EMAIL.value())).isZero();
        assertThat(mentions("Zofia Erasure")).isZero();
        // the trainer keeps everything but what belonged to the erased client: the link, the invite and the conversation
        assertThat(references(EWA) + references(PIOTR)).isEqualTo(othersBefore);
        assertThat(accounts.findById(MAREK)).isPresent();
        assertThat(after.get("exercises")).isEqualTo(before.get("exercises"));
        assertThat(after.get("plan_build_requests")).isEqualTo(before.get("plan_build_requests"));
    }

    @Test
    void shouldRefuseATrainerWithClients() {
        assertThatThrownBy(() -> accounts.delete(MAREK)).isInstanceOf(TrainerHasClientsException.class);
    }

    @Test
    void shouldDeleteATrainerWithTheirExercisesInvitesAndBuildRequests_onceNoClientIsLeft() {
        deleteClient(ZOFIA, ZOFIA_EMAIL);
        deleteClient(PIOTR, new Email("piotr.control@example.com"));
        int ewaBefore = references(EWA);

        accounts.delete(MAREK);

        assertThat(references(MAREK)).isZero();
        assertThat(mentions("Marek")).isZero();
        assertThat(references(EWA)).isEqualTo(ewaBefore);
    }
}
