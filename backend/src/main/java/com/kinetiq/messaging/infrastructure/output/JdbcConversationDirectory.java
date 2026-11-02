package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.LastMessage;
import com.kinetiq.messaging.domain.model.PlanWeek;
import com.kinetiq.messaging.domain.output.ConversationDirectory;
import com.kinetiq.quiz.domain.model.Goal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the accounts, the quiz answers, the plans and the messages: one query, and no class of those features
@Repository
@RequiredArgsConstructor
class JdbcConversationDirectory implements ConversationDirectory {

    // the conversation is the one with the client's current trainer, so a new trainer never sees earlier history
    private static final String ROWS = """
            select a.id as client_id,
                   case when a.id = me.id then t.name else a.name end as name,
                   coalesce(p.goal, q.goal) as goal,
                   case when p.first_week is not null
                        then greatest(1, ((now() at time zone a.time_zone)::date - p.first_week) / 7 + 1)
                   end as current_week,
                   p.weeks as plan_weeks,
                   u.unread,
                   l.preview,
                   l.sender_id,
                   l.received_at
            from (select ?::uuid as id) me
            join accounts a on a.role = 'CLIENT' and me.id in (a.id, a.trainer_id)
            join accounts t on t.id = a.trainer_id
            left join quiz_submissions q on q.client_id = a.id
            left join plans p on p.client_id = a.id
            left join conversations c on c.client_id = a.id and c.trainer_id = a.trainer_id
            left join conversation_reads r on r.conversation_id = c.id and r.account_id = me.id
            left join lateral (
                select left(m.text, 140) as preview, m.sender_id, m.received_at
                from messages m
                where m.conversation_id = c.id
                order by m.received_at desc, m.id desc
                limit 1
            ) l on true
            cross join lateral (
                select count(*) as unread
                from messages m
                where m.conversation_id = c.id
                  and m.sender_id <> me.id
                  and (r.last_read_at is null or m.received_at > r.last_read_at)
            ) u
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<ConversationOverview> overviewOf(AccountId person) {
        return jdbc.query(
                "select * from (" + ROWS + ") as r order by received_at desc nulls last, lower(name), client_id",
                (rows, rowNumber) -> overview(rows, person),
                person.value());
    }

    @Override
    public int unreadTotal(AccountId person) {
        Integer total = jdbc.queryForObject(
                "select coalesce(sum(unread), 0)::int from (" + ROWS + ") as conversation_rows",
                Integer.class,
                person.value());
        return total == null ? 0 : total;
    }

    private static ConversationOverview overview(ResultSet rows, AccountId person) throws SQLException {
        String goal = rows.getString("goal");
        Integer currentWeek = rows.getObject("current_week", Integer.class);
        return new ConversationOverview(
                new AccountId(rows.getObject("client_id", UUID.class)),
                rows.getString("name"),
                goal == null ? null : Goal.valueOf(goal),
                currentWeek == null ? null : new PlanWeek(currentWeek, rows.getInt("plan_weeks")),
                rows.getInt("unread"),
                lastMessage(rows, person));
    }

    private static LastMessage lastMessage(ResultSet rows, AccountId person) throws SQLException {
        OffsetDateTime receivedAt = rows.getObject("received_at", OffsetDateTime.class);
        if (receivedAt == null) {
            return null;
        }
        return new LastMessage(
                rows.getString("preview"),
                person.value().equals(rows.getObject("sender_id", UUID.class)),
                receivedAt.toInstant());
    }
}
