package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.plan.domain.model.DraftedPlan;
import com.kinetiq.plan.domain.output.DraftedPlanDirectory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model that reads the accounts by SQL, as JdbcClientDirectory does, and no class of the account feature
@Repository
@RequiredArgsConstructor
class JdbcDraftedPlanDirectory implements DraftedPlanDirectory {

    // the trainer is part of the join, so another trainer's client can never come back
    private static final String DRAFTS_OF = """
            select p.client_id, a.name, p.automatic_draft, p.weeks, p.created_at,
                   (select count(*) from plan_sessions s where s.plan_id = p.id) as sessions
            from plans p
            join accounts a on a.id = p.client_id and a.trainer_id = ?
            where p.first_week is null
            order by p.created_at, p.id
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<DraftedPlan> findOf(AccountId trainer) {
        return jdbc.query(DRAFTS_OF, JdbcDraftedPlanDirectory::draft, trainer.value());
    }

    private static DraftedPlan draft(ResultSet rows, int rowNumber) throws SQLException {
        return new DraftedPlan(
                new AccountId(rows.getObject("client_id", UUID.class)),
                rows.getString("name"),
                kind(rows.getString("automatic_draft")),
                rows.getInt("weeks"),
                rows.getInt("sessions"),
                rows.getObject("created_at", OffsetDateTime.class).toInstant());
    }

    // a draft the trainer has saved carries no mark
    private static DraftedPlan.Kind kind(String automaticDraft) {
        return switch (automaticDraft) {
            case "BUILT" -> DraftedPlan.Kind.BUILT;
            case "FAILED" -> DraftedPlan.Kind.FAILED;
            case null, default -> DraftedPlan.Kind.SAVED;
        };
    }
}
