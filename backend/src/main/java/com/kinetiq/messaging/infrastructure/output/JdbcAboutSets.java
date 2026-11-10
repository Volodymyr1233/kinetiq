package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.messaging.domain.output.AboutSets;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the session logs and the exercises: one query, and no class of those features' infrastructure
@Repository
@RequiredArgsConstructor
class JdbcAboutSets implements AboutSets {

    // a session log keeps its own copy of the session's name and entries, so a plan saved again changes nothing here
    private static final String SETS = """
            select sl.id, sl.session_log_id, s.session_name, s.log_date, x.name, x.name_pl,
                   sl.entry_position, sl.set_number, sl.rpe
            from set_logs sl
            join session_logs s on s.id = sl.session_log_id
            join session_log_entries e on e.session_log_id = sl.session_log_id and e.position = sl.entry_position
            join exercises x on x.id = e.exercise_id
            """;

    private final JdbcTemplate jdbc;

    @Override
    public Optional<AboutSet> ofSender(AccountId sender, SetLogId setLog) {
        return jdbc
                .query(
                        SETS + "where sl.id = ? and s.client_id = ?",
                        (rows, rowNumber) -> aboutSet(rows),
                        setLog.value(),
                        sender.value())
                .stream()
                .findFirst();
    }

    /** Returns the sets of the given set log ids, by id, whoever logged them: for messages that are stored already. */
    Map<UUID, AboutSet> byIds(Collection<UUID> setLogs) {
        if (setLogs.isEmpty()) {
            return Map.of();
        }
        return jdbc
                .query(
                        SETS + "where sl.id = any(?)",
                        statement -> statement.setArray(
                                1, statement.getConnection().createArrayOf("uuid", setLogs.toArray())),
                        (rows, rowNumber) -> aboutSet(rows))
                .stream()
                .collect(Collectors.toMap(set -> set.setLog().value(), Function.identity()));
    }

    private static AboutSet aboutSet(ResultSet rows) throws SQLException {
        return new AboutSet(
                new SetLogId(rows.getObject("id", UUID.class)),
                new SessionLogId(rows.getObject("session_log_id", UUID.class)),
                rows.getString("session_name"),
                rows.getObject("log_date", LocalDate.class),
                new LocalizedText(rows.getString("name"), rows.getString("name_pl")),
                rows.getInt("entry_position"),
                rows.getInt("set_number"),
                rows.getInt("rpe"));
    }
}
