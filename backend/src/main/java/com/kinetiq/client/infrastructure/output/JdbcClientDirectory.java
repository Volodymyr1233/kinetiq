package com.kinetiq.client.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.quiz.domain.model.Goal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the accounts and the quiz answers: one query, and no class of those features
@Repository
@RequiredArgsConstructor
class JdbcClientDirectory implements ClientDirectory {

    private static final String CLIENTS_OF = """
            select a.id, a.name, q.goal
            from accounts a
            left join quiz_submissions q on q.client_id = a.id
            where a.trainer_id = ?
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<ClientSummary> findClientsOf(AccountId trainer) {
        return jdbc.query(CLIENTS_OF + "order by lower(a.name), a.id", this::summary, trainer.value());
    }

    // the trainer is part of the condition, so another trainer's client is as absent as an unknown one
    @Override
    public Optional<ClientSummary> findClient(AccountId trainer, AccountId client) {
        return jdbc.query(CLIENTS_OF + "and a.id = ?", this::summary, trainer.value(), client.value()).stream()
                .findFirst();
    }

    private ClientSummary summary(ResultSet rows, int rowNumber) throws SQLException {
        String goal = rows.getString("goal");
        return new ClientSummary(
                new AccountId(rows.getObject("id", UUID.class)),
                rows.getString("name"),
                goal == null ? null : Goal.valueOf(goal),
                // plans arrive with the plan stories; until then a submitted quiz is the plan to review
                goal == null ? PlanState.NO_QUIZ : PlanState.PLAN_TO_REVIEW);
    }
}
