package com.kinetiq.sessionlog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.sessionlog.domain.exception.NotFinishedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SessionSummaryTest {

    private static final Instant START = Instant.parse("2026-11-07T10:00:00Z");
    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();
    private static final Map<ExerciseId, Measure> MEASURES = Map.of(SQUAT, Measure.REPS, PLANK, Measure.SECONDS);

    private static SessionLog started(LoggedEntry... entries) {
        return SessionLog.start(AccountId.random(), LocalDate.parse("2026-11-07"), "Lower", List.of(entries), START);
    }

    private static LoggedEntry squat(int sets) {
        return new LoggedEntry(SQUAT, sets, 5, new BigDecimal("100.0"), 8, 120);
    }

    private static LoggedEntry plank(int sets) {
        return new LoggedEntry(PLANK, sets, 45, null, 7, 60);
    }

    private static void log(SessionLog log, int entry, int number, String load, int amount, int rpe) {
        log.logSet(
                new SetLogId(UUID.randomUUID()),
                entry,
                number,
                new SetValues(load == null ? null : new BigDecimal(load), amount, rpe),
                START);
    }

    private static SessionSummary finished(SessionLog log, Instant at) {
        log.finish(at);
        return log.summary(MEASURES);
    }

    @Test
    void shouldAddVolumeAndAverageRpe_fromTheLoggedSets() {
        SessionLog log = started(squat(3), plank(2));
        log(log, 0, 1, "100.0", 5, 7);
        log(log, 0, 2, "100.0", 5, 8);
        log(log, 0, 3, "102.5", 5, 9);

        SessionSummary summary = finished(log, START.plusSeconds(45 * 60));

        assertThat(summary.setsLogged()).isEqualTo(3);
        assertThat(summary.setsPlanned()).isEqualTo(5);
        assertThat(summary.volumeKg()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("1512.5"));
        assertThat(summary.averageRpe()).isEqualByComparingTo("8.0");
        assertThat(summary.minutes()).isEqualTo(45);
    }

    @Test
    void shouldLeaveOutSetsInSecondsAndWithoutALoad_fromTheVolume() {
        SessionLog log = started(squat(2), plank(1));
        log(log, 0, 1, null, 5, 6);
        log(log, 0, 2, "100.0", 5, 8);
        log(log, 1, 1, "10.0", 45, 7);

        SessionSummary summary = finished(log, START.plusSeconds(600));

        assertThat(summary.setsLogged()).isEqualTo(3);
        assertThat(summary.volumeKg()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("500.0"));
    }

    @Test
    void shouldHaveNoVolume_whenNoSetCounts() {
        SessionLog log = started(squat(1), plank(1));
        log(log, 0, 1, null, 5, 8);
        log(log, 1, 1, null, 45, 6);

        assertThat(finished(log, START.plusSeconds(600)).volumeKg()).isEmpty();
    }

    @Test
    void shouldGiveTheRpeOfTheOnlySet() {
        SessionLog log = started(squat(3));
        log(log, 0, 1, "100.0", 5, 9);

        assertThat(finished(log, START.plusSeconds(600)).averageRpe()).isEqualByComparingTo("9.0");
    }

    @Test
    void shouldRoundTheAverageRpeHalfUp() {
        SessionLog log = started(squat(3));
        log(log, 0, 1, "100.0", 5, 7);
        log(log, 0, 2, "100.0", 5, 8);
        log(log, 0, 3, "100.0", 5, 8);

        assertThat(finished(log, START.plusSeconds(600)).averageRpe()).isEqualByComparingTo("7.7");
    }

    @Test
    void shouldCountWholeMinutes_butAtLeastOne() {
        SessionLog quick = started(squat(1));
        log(quick, 0, 1, "100.0", 5, 8);
        SessionLog slow = started(squat(1));
        log(slow, 0, 1, "100.0", 5, 8);

        assertThat(finished(quick, START.plusSeconds(10)).minutes()).isEqualTo(1);
        assertThat(finished(slow, START.plusSeconds(52 * 60 + 40)).minutes()).isEqualTo(52);
    }

    @Test
    void shouldFollowTheSetsAsTheyAreNow_whenOneChangesAfterFinishing() {
        SessionLog log = started(squat(1));
        SetLogId id = new SetLogId(UUID.randomUUID());
        log.logSet(id, 0, 1, new SetValues(new BigDecimal("100.0"), 5, 8), START);
        log.finish(START.plusSeconds(600));

        log.logSet(id, 0, 1, new SetValues(new BigDecimal("110.0"), 5, 9), START.plusSeconds(900));

        SessionSummary summary = log.summary(MEASURES);
        assertThat(summary.volumeKg()).hasValueSatisfying(v -> assertThat(v).isEqualByComparingTo("550.0"));
        assertThat(summary.averageRpe()).isEqualByComparingTo("9.0");
    }

    @Test
    void shouldRefuseAnUnfinishedLog() {
        SessionLog log = started(squat(1));

        assertThatThrownBy(() -> log.summary(MEASURES)).isInstanceOf(NotFinishedException.class);
    }
}
