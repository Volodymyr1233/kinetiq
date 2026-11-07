package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftingKind;
import com.kinetiq.plan.domain.output.PlanDraftingRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class PlanDraftingRepositoryImpl implements PlanDraftingRepository {

    private final JdbcTemplate jdbc;

    @Override
    public void start(AccountId client, Instant at, DraftingKind kind) {
        jdbc.update(
                "insert into plan_draftings (client_id, started_at, rebuild) values (?, ?, ?)",
                client.value(),
                Timestamp.from(at),
                kind == DraftingKind.REBUILD);
    }

    // one statement, so the job and the sweep cannot both learn they removed the row
    @Override
    public Optional<DraftingKind> remove(AccountId client) {
        return jdbc
                .query(
                        "delete from plan_draftings where client_id = ? returning rebuild",
                        (rows, rowNumber) -> rows.getBoolean("rebuild") ? DraftingKind.REBUILD : DraftingKind.FIRST,
                        client.value())
                .stream()
                .findFirst();
    }

    @Override
    public boolean exists(AccountId client) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "select exists (select 1 from plan_draftings where client_id = ?)", Boolean.class, client.value()));
    }

    @Override
    public List<AccountId> startedBefore(Instant instant) {
        return jdbc.query(
                "select client_id from plan_draftings where started_at < ?",
                (rows, rowNumber) -> new AccountId(rows.getObject("client_id", UUID.class)),
                Timestamp.from(instant));
    }
}
