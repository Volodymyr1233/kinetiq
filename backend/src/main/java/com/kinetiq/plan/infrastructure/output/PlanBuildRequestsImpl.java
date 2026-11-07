package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.output.PlanBuildRequests;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PlanBuildRequestsImpl implements PlanBuildRequests {

    private final JdbcTemplate jdbc;

    // a transaction-scoped lock on the trainer's id, so a count and the insert after it cannot interleave (research R4)
    @Override
    public void lock(AccountId trainer) {
        jdbc.queryForList("select pg_advisory_xact_lock(hashtextextended(?, 0))", trainer.value().toString());
    }

    @Override
    public List<Instant> since(AccountId trainer, Instant from) {
        return jdbc.query(
                """
                select requested_at from plan_build_requests
                where trainer_id = ? and requested_at > ?
                order by requested_at
                """,
                (rows, rowNumber) -> rows.getTimestamp("requested_at").toInstant(),
                trainer.value(),
                Timestamp.from(from));
    }

    @Override
    public void record(AccountId trainer, Instant at, Instant forgetBefore) {
        jdbc.update(
                "delete from plan_build_requests where trainer_id = ? and requested_at < ?",
                trainer.value(),
                Timestamp.from(forgetBefore));
        jdbc.update(
                "insert into plan_build_requests (id, trainer_id, requested_at) values (?, ?, ?)",
                UUID.randomUUID(),
                trainer.value(),
                Timestamp.from(at));
    }
}
