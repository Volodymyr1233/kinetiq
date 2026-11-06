package com.kinetiq.client.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.model.ClientSummary;
import com.kinetiq.client.domain.model.PlanState;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.quiz.domain.model.Goal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the accounts, the quiz answers and the plans: one query, and no class of those features
@Repository
@RequiredArgsConstructor
class JdbcClientDirectory implements ClientDirectory {

    private static final String CLIENTS_OF = """
            select a.id, a.name, a.time_zone, q.goal as quiz_goal, p.goal as plan_goal, p.first_week, p.automatic_draft
            from accounts a
            left join quiz_submissions q on q.client_id = a.id
            left join plans p on p.client_id = a.id
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
        String quizGoal = rows.getString("quiz_goal");
        String planGoal = rows.getString("plan_goal");
        String goal = planGoal == null ? quizGoal : planGoal;
        return new ClientSummary(
                new AccountId(rows.getObject("id", UUID.class)),
                rows.getString("name"),
                goal == null ? null : Goal.valueOf(goal),
                planState(
                        quizGoal,
                        planGoal,
                        rows.getObject("first_week", LocalDate.class),
                        rows.getString("automatic_draft")),
                ZoneId.of(rows.getString("time_zone")));
    }

    // a generated draft waits for the same review as answers with no plan yet; the mark goes with the first save
    private static PlanState planState(String quizGoal, String planGoal, LocalDate firstWeek, String automaticDraft) {
        if (planGoal != null) {
            if (firstWeek != null) {
                return PlanState.PUBLISHED;
            }
            return switch (automaticDraft) {
                case "FAILED" -> PlanState.DRAFT_FAILED;
                case "BUILT" -> PlanState.PLAN_TO_REVIEW;
                case null, default -> PlanState.DRAFT;
            };
        }
        return quizGoal == null ? PlanState.NO_QUIZ : PlanState.PLAN_TO_REVIEW;
    }
}
