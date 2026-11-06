package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.output.PlanDraftingRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PlanDraftingRepositoryImpl implements PlanDraftingRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void start(AccountId client, Instant at) {
        jdbc.update(
                "insert into plan_draftings (client_id, started_at) values (?, ?)",
                client.value(),
                Timestamp.from(at));
    }

    @Override
    public boolean remove(AccountId client) {
        return jdbc.update("delete from plan_draftings where client_id = ?", client.value()) > 0;
    }

    @Override
    public List<AccountId> startedBefore(Instant instant) {
        return jdbc.query(
                "select client_id from plan_draftings where started_at < ?",
                (rows, rowNumber) -> new AccountId(rows.getObject("client_id", UUID.class)),
                Timestamp.from(instant));
    }
}
