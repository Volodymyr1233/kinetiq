package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the plan and log tables of other features, as JdbcDraftedPlanDirectory is, so no class of theirs is used
@Repository
@RequiredArgsConstructor
class JdbcTrainingHistory implements TrainingHistory {

    private static final String SCHEDULED = """
            select p.client_id, a.time_zone, p.first_week, p.weeks,
                   (select max(w.week_start) from weekly_checks w where w.client_id = p.client_id) as last_checked
            from plans p
            join accounts a on a.id = p.client_id
            where p.first_week is not null
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<ScheduledClient> clientsWithPublishedPlan() {
        return jdbc.query(SCHEDULED + " order by p.client_id", JdbcTrainingHistory::scheduled);
    }

    @Override
    public Optional<ScheduledClient> scheduledClient(AccountId client) {
        return jdbc.query(SCHEDULED + " and p.client_id = ?", JdbcTrainingHistory::scheduled, client.value()).stream()
                .findFirst();
    }

    @Override
    public boolean claimWeek(AccountId client, LocalDate week, Instant now) {
        // the primary key makes this the once-a-week guarantee, also between two runs at the same time
        return jdbc.update(
                        "insert into weekly_checks (client_id, week_start, checked_at) values (?, ?, ?) on conflict do nothing",
                        client.value(),
                        week,
                        OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                == 1;
    }

    private static ScheduledClient scheduled(ResultSet rows, int rowNumber) throws SQLException {
        return new ScheduledClient(
                new AccountId(rows.getObject("client_id", UUID.class)),
                ZoneId.of(rows.getString("time_zone")),
                rows.getObject("first_week", LocalDate.class),
                rows.getInt("weeks"),
                Optional.ofNullable(rows.getObject("last_checked", LocalDate.class)));
    }
}
