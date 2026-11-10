package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.LoggedSet;
import com.kinetiq.planchange.domain.model.ScheduledClient;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.output.TrainingHistory;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

// a read model over the plan and log tables of other features, as JdbcDraftedPlanDirectory is, so no class of theirs is used
@Slf4j
@Repository
@RequiredArgsConstructor
class JdbcTrainingHistory implements TrainingHistory {

    private static final String SCHEDULED = """
            select p.client_id, a.time_zone, p.first_week, p.weeks,
                   (select max(w.week_start) from weekly_checks w where w.client_id = p.client_id) as last_checked
            from plans p
            join accounts a on a.id = p.client_id
            where p.first_week is not null
            """;

    private static final int DAYS_PER_WEEK = 7;

    // the exercise of a set is on the copy of the planned entry that the session log keeps
    private static final String SETS = """
            select l.log_date, e.exercise_id, s.load_kg, s.rpe
            from set_logs s
            join session_logs l on l.id = s.session_log_id
            join session_log_entries e on e.session_log_id = s.session_log_id and e.position = s.entry_position
            where l.client_id = ? and l.log_date between ? and ?
            """;

    private final JdbcTemplate jdbc;

    @Override
    public List<ScheduledClient> clientsWithPublishedPlan() {
        return jdbc.query(SCHEDULED + " order by p.client_id", JdbcTrainingHistory::scheduled).stream()
                .filter(Objects::nonNull)
                .toList();
    }

    @Override
    public Optional<ScheduledClient> scheduledClient(AccountId client) {
        return jdbc.query(SCHEDULED + " and p.client_id = ?", JdbcTrainingHistory::scheduled, client.value()).stream()
                .filter(Objects::nonNull)
                .findFirst();
    }

    // every query is scoped to the one client; days and weeks are matched by date, as SessionStateService does
    @Override
    public List<WeekTraining> weeks(AccountId client, LocalDate firstMonday, int count) {
        LocalDate lastDay = firstMonday.plusDays((long) count * DAYS_PER_WEEK - 1);
        List<PlanDay> planDays = jdbc.query(
                """
                select s.day_of_week, p.published_on
                from plan_sessions s
                join plans p on p.id = s.plan_id
                where p.client_id = ?
                """,
                (rows, rowNumber) -> new PlanDay(
                        DayOfWeek.valueOf(rows.getString("day_of_week")),
                        rows.getObject("published_on", LocalDate.class)),
                client.value());
        Set<LocalDate> finished = Set.copyOf(jdbc.query(
                "select log_date from session_logs where client_id = ? and finished_at is not null"
                        + " and log_date between ? and ?",
                (rows, rowNumber) -> rows.getObject("log_date", LocalDate.class),
                client.value(),
                firstMonday,
                lastDay));
        List<List<LoggedSet>> sets = new ArrayList<>();
        for (int week = 0; week < count; week++) {
            sets.add(new ArrayList<>());
        }
        jdbc.query(
                SETS,
                rows -> {
                    LocalDate date = rows.getObject("log_date", LocalDate.class);
                    int week = (int) (ChronoUnit.DAYS.between(firstMonday, date) / DAYS_PER_WEEK);
                    sets.get(week)
                            .add(new LoggedSet(
                                    new ExerciseId(rows.getObject("exercise_id", UUID.class)),
                                    rows.getBigDecimal("load_kg"),
                                    rows.getInt("rpe")));
                },
                client.value(),
                firstMonday,
                lastDay);
        List<WeekTraining> weeks = new ArrayList<>();
        for (int week = 0; week < count; week++) {
            LocalDate monday = firstMonday.plusWeeks(week);
            List<LocalDate> planned = planDays.stream()
                    .map(day -> monday.plusDays(day.day().getValue() - 1L))
                    .filter(date -> !date.isBefore(planDays.getFirst().publishedOn()))
                    .toList();
            List<DayOfWeek> finishedDays = finished.stream()
                    .filter(date -> !date.isBefore(monday) && date.isBefore(monday.plusWeeks(1)))
                    .map(LocalDate::getDayOfWeek)
                    .sorted()
                    .toList();
            weeks.add(new WeekTraining(
                    monday,
                    planDays.size(),
                    planned.size(),
                    (int) planned.stream().filter(finished::contains).count(),
                    sets.get(week),
                    finishedDays));
        }
        return weeks;
    }

    @Override
    public boolean claimWeek(AccountId client, LocalDate week, Instant now) {
        // the primary key makes this the once-a-week guarantee, also between two runs at the same time
        return jdbc.update(
                        "insert into weekly_checks (client_id, week_start, checked_at) values (?, ?, ?) on conflict do nothing",
                        client.value(),
                        week,
                        OffsetDateTime.ofInstant(now, ZoneOffset.UTC))
                == 1;
    }

    // a plan day carries the publishing day of its plan, which is the same on every row
    private record PlanDay(DayOfWeek day, LocalDate publishedOn) {}

    // a time zone that is not valid skips that client only, so it cannot hold up the check of every other client
    private static ScheduledClient scheduled(ResultSet rows, int rowNumber) throws SQLException {
        ZoneId zone;
        try {
            zone = ZoneId.of(rows.getString("time_zone"));
        } catch (DateTimeException exception) {
            log.warn("Client left out of the weekly check, time zone is not valid");
            return null;
        }
        return new ScheduledClient(
                new AccountId(rows.getObject("client_id", UUID.class)),
                zone,
                rows.getObject("first_week", LocalDate.class),
                rows.getInt("weeks"),
                Optional.ofNullable(rows.getObject("last_checked", LocalDate.class)));
    }
}
