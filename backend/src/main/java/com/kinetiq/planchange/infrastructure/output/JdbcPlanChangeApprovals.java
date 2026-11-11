package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.output.PlanChangeApprovals;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the accounts table of another feature, as JdbcTrainingHistory is, so no class of theirs is used
@Repository
@RequiredArgsConstructor
class JdbcPlanChangeApprovals implements PlanChangeApprovals {

    // the one that changed last first: it was created, or it ended
    private static final String FOR_TRAINER = """
            select p.id, a.name
            from plan_changes p
            join accounts a on a.id = p.client_id
            where a.trainer_id = ?
              and (p.state in ('PENDING', 'ACCEPTED_BY_ONE') or p.ended_at >= ?)
            order by coalesce(p.ended_at, p.created_at) desc, p.id
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<Entry> forTrainer(AccountId trainer, Instant endedSince) {
        return jdbc.query(
                FOR_TRAINER,
                (rows, row) -> new Entry(new PlanChangeId(rows.getObject("id", UUID.class)), rows.getString("name")),
                trainer.value(),
                Timestamp.from(endedSince));
    }
}
