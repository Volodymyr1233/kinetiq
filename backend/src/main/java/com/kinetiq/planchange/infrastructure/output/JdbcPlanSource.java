package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.planchange.domain.model.EntrySnapshot;
import com.kinetiq.planchange.domain.model.ExerciseLabel;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import com.kinetiq.planchange.domain.output.PlanSource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the plan and exercise tables of other features, as JdbcTrainingHistory is
@Repository
@RequiredArgsConstructor
class JdbcPlanSource implements PlanSource {

    private final JdbcTemplate jdbc;

    private record PlanRow(UUID id, long version, LocalDate firstWeek, int weeks) {}

    @Override
    public Optional<PlanSnapshot> currentPlan(AccountId client) {
        Optional<PlanRow> plan = jdbc.query(
                        "select id, version, first_week, weeks from plans where client_id = ? and first_week is not null",
                        (rows, rowNumber) -> new PlanRow(
                                rows.getObject("id", UUID.class),
                                rows.getLong("version"),
                                rows.getObject("first_week", LocalDate.class),
                                rows.getInt("weeks")),
                        client.value())
                .stream()
                .findFirst();
        return plan.map(row -> new PlanSnapshot(row.version(), row.firstWeek(), row.weeks(), sessions(row.id())));
    }

    private List<SessionSnapshot> sessions(UUID plan) {
        Map<DayOfWeek, String> names = new LinkedHashMap<>();
        Map<DayOfWeek, List<EntrySnapshot>> entries = new HashMap<>();
        jdbc.query(
                "select day_of_week, name from plan_sessions where plan_id = ?",
                rows -> {
                    DayOfWeek day = DayOfWeek.valueOf(rows.getString("day_of_week"));
                    names.put(day, rows.getString("name"));
                    entries.put(day, new ArrayList<>());
                },
                plan);
        jdbc.query(
                """
                select s.day_of_week, e.exercise_id, x.category, x.measure, e.sets, e.amount, e.load_kg, e.rpe,
                       e.rest_seconds
                from plan_session_entries e
                join plan_sessions s on s.id = e.session_id
                join exercises x on x.id = e.exercise_id
                where s.plan_id = ?
                order by s.day_of_week, e.position
                """,
                rows -> {
                    entries.get(DayOfWeek.valueOf(rows.getString("day_of_week")))
                            .add(new EntrySnapshot(
                                    new ExerciseId(rows.getObject("exercise_id", UUID.class)),
                                    Category.valueOf(rows.getString("category")),
                                    Measure.valueOf(rows.getString("measure")),
                                    rows.getInt("sets"),
                                    rows.getInt("amount"),
                                    rows.getBigDecimal("load_kg"),
                                    rows.getInt("rpe"),
                                    rows.getInt("rest_seconds")));
                },
                plan);
        return names.entrySet().stream()
                .map(session -> new SessionSnapshot(session.getKey(), session.getValue(), entries.get(session.getKey())))
                .toList();
    }

    @Override
    public Map<ExerciseId, ExerciseLabel> exercises(Set<ExerciseId> exercises, Language language) {
        if (exercises.isEmpty()) {
            return Map.of();
        }
        Map<ExerciseId, ExerciseLabel> names = new HashMap<>();
        new NamedParameterJdbcTemplate(jdbc)
                .query(
                        "select id, name, name_pl, measure from exercises where id in (:ids)",
                        new MapSqlParameterSource(
                                "ids", exercises.stream().map(ExerciseId::value).toList()),
                        rows -> {
                            String polish = rows.getString("name_pl");
                            names.put(
                                    new ExerciseId(rows.getObject("id", UUID.class)),
                                    new ExerciseLabel(
                                            language == Language.PL && polish != null && !polish.isBlank()
                                                    ? polish
                                                    : rows.getString("name"),
                                            Measure.valueOf(rows.getString("measure"))));
                        });
        return Map.copyOf(names);
    }
}
