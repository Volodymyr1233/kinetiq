package com.kinetiq.overview.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.overview.domain.model.ClientTraining;
import com.kinetiq.overview.domain.model.ClientTraining.DayCheckIn;
import com.kinetiq.overview.domain.model.ClientTraining.DayLog;
import com.kinetiq.overview.domain.model.ClientTraining.LastLog;
import com.kinetiq.overview.domain.model.PlanDates;
import com.kinetiq.overview.domain.model.RpeTotal;
import com.kinetiq.overview.domain.output.TrainingRecords;
import com.kinetiq.readiness.domain.model.Answers;
import com.kinetiq.readiness.domain.model.TargetChoice;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

// a read model over the plan, log and check-in tables of other features, as JdbcTrainingLog is: four queries for all
// of a trainer's clients, each bounded by the trainer, whatever the number of clients
@Repository
@RequiredArgsConstructor
class JdbcTrainingRecords implements TrainingRecords {

    private static final String PLANS = """
            select p.client_id, p.first_week, p.published_on, p.weeks, s.day_of_week
            from plans p
            join accounts a on a.id = p.client_id
            left join plan_sessions s on s.plan_id = p.id
            where a.trainer_id = ? and p.first_week is not null
            """;

    private static final String DAY_LOGS = """
            select l.client_id, l.log_date, l.finished_at is not null as finished,
                   coalesce(sum(s.rpe), 0) as rpe_sum, count(s.id) as sets
            from session_logs l
            join accounts a on a.id = l.client_id
            left join set_logs s on s.session_log_id = l.id
            where a.trainer_id = ? and l.log_date >= ?
            group by l.id
            """;

    private static final String LAST_LOGS = """
            select distinct on (l.client_id) l.client_id, s.logged_at, l.log_date, l.session_name
            from set_logs s
            join session_logs l on l.id = s.session_log_id
            join accounts a on a.id = l.client_id
            where a.trainer_id = ?
            order by l.client_id, s.logged_at desc
            """;

    private static final String CHECK_INS = """
            select c.client_id, c.check_in_date, c.sleep, c.soreness, c.stress, c.target_choice
            from check_ins c
            join accounts a on a.id = c.client_id
            where a.trainer_id = ? and c.check_in_date >= ?
            """;

    private final JdbcTemplate jdbc;

    @Override
    public Map<AccountId, ClientTraining> forTrainer(AccountId trainer, LocalDate since) {
        Map<AccountId, Builder> clients = new HashMap<>();
        jdbc.query(PLANS, (RowCallbackHandler) rows -> {
            Builder client = clients.computeIfAbsent(id(rows), key -> new Builder());
            client.firstWeek = rows.getObject("first_week", LocalDate.class);
            client.publishedOn = rows.getObject("published_on", LocalDate.class);
            client.weeks = rows.getInt("weeks");
            String day = rows.getString("day_of_week");
            if (day != null) {
                client.days.add(DayOfWeek.valueOf(day));
            }
        }, trainer.value());
        jdbc.query(
                DAY_LOGS,
                (RowCallbackHandler) rows -> clients.computeIfAbsent(id(rows), key -> new Builder())
                        .logs
                        .put(
                                rows.getObject("log_date", LocalDate.class),
                                new DayLog(
                                        rows.getBoolean("finished"),
                                        new RpeTotal(rows.getInt("rpe_sum"), rows.getInt("sets")))),
                trainer.value(),
                since);
        jdbc.query(
                LAST_LOGS,
                (RowCallbackHandler) rows -> clients.computeIfAbsent(id(rows), key -> new Builder()).lastLog = new LastLog(
                        rows.getTimestamp("logged_at").toInstant(),
                        rows.getObject("log_date", LocalDate.class),
                        rows.getString("session_name")),
                trainer.value());
        jdbc.query(
                CHECK_INS,
                (RowCallbackHandler) rows -> clients.computeIfAbsent(id(rows), key -> new Builder())
                        .checkIns
                        .put(
                                rows.getObject("check_in_date", LocalDate.class),
                                new DayCheckIn(
                                        new Answers(
                                                rows.getInt("sleep"), rows.getInt("soreness"), rows.getInt("stress")),
                                        TargetChoice.valueOf(rows.getString("target_choice")))),
                trainer.value(),
                since);
        Map<AccountId, ClientTraining> training = new HashMap<>();
        clients.forEach((client, builder) -> training.put(client, builder.build()));
        return Map.copyOf(training);
    }

    private static AccountId id(ResultSet rows) throws SQLException {
        return new AccountId(rows.getObject("client_id", UUID.class));
    }

    private static final class Builder {

        private LocalDate firstWeek;
        private LocalDate publishedOn;
        private int weeks;
        private final Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        private final Map<LocalDate, DayLog> logs = new HashMap<>();
        private LastLog lastLog;
        private final Map<LocalDate, DayCheckIn> checkIns = new HashMap<>();

        private ClientTraining build() {
            PlanDates plan = firstWeek == null ? null : new PlanDates(firstWeek, publishedOn, weeks, new HashSet<>(days));
            return new ClientTraining(plan, logs, lastLog, checkIns);
        }
    }
}
