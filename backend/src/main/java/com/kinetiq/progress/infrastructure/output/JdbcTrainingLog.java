package com.kinetiq.progress.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.progress.domain.model.LoggedExercise;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.progress.domain.output.TrainingLog;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the log tables of the sessionlog feature, as JdbcTrainingHistory is, so no class of theirs is used
@Repository
@RequiredArgsConstructor
class JdbcTrainingLog implements TrainingLog {

    // the exercise of a set is on the session's copy of the entry; the window sums run before distinct on keeps the top
    private static final String LOGGED_EXERCISES = """
            select * from (
                select distinct on (l.id, e.exercise_id)
                       l.log_date, e.exercise_id, s.load_kg, s.amount, s.rpe,
                       sum(s.rpe) over session_exercise as rpe_sum,
                       count(*) over session_exercise as sets
                from set_logs s
                join session_logs l on l.id = s.session_log_id
                join session_log_entries e on e.session_log_id = s.session_log_id and e.position = s.entry_position
                where l.client_id = ?
                window session_exercise as (partition by l.id, e.exercise_id)
                order by l.id, e.exercise_id, s.load_kg desc nulls last, s.amount desc, s.entry_position, s.set_number
            ) top_sets
            order by log_date, exercise_id
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<LoggedExercise> loggedExercises(AccountId client) {
        return jdbc.query(
                LOGGED_EXERCISES,
                (rows, rowNumber) -> new LoggedExercise(
                        rows.getObject("log_date", LocalDate.class),
                        new ExerciseId(rows.getObject("exercise_id", UUID.class)),
                        new TopSet(rows.getBigDecimal("load_kg"), rows.getInt("amount"), rows.getInt("rpe")),
                        rows.getInt("rpe_sum"),
                        rows.getInt("sets")),
                client.value());
    }

    @Override
    public Set<LocalDate> finishedDays(AccountId client) {
        return Set.copyOf(jdbc.query(
                "select log_date from session_logs where client_id = ? and finished_at is not null",
                (rows, rowNumber) -> rows.getObject("log_date", LocalDate.class),
                client.value()));
    }
}
