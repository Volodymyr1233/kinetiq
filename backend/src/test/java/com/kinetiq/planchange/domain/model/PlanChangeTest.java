package com.kinetiq.planchange.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PlanChangeTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final ExerciseId SQUAT = ExerciseId.random();

    private static final TargetChange CHANGE = new TargetChange(
            new EntryRef(DayOfWeek.MONDAY, 0),
            SQUAT,
            "Lower body",
            Target.AMOUNT,
            new BigDecimal("10.0"),
            new BigDecimal("9.0"));

    private static PlanChange createdAt(String instant, ZoneId zone) {
        DuePlanChange due = new DuePlanChange(
                AccountId.random(),
                zone,
                Rule.HIGH_RPE,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                Set.of(SQUAT),
                List.of(),
                List.of());
        return PlanChange.create(due, PlanChangeContent.ofChanges(List.of(CHANGE)), 3, Instant.parse(instant));
    }

    @Test
    void shouldTakeEffectTheNextMonday_whenCreatedOnAMonday() {
        assertThat(createdAt("2026-11-09T02:00:00Z", WARSAW).getEffectiveWeek()).isEqualTo(LocalDate.parse("2026-11-16"));
    }

    @Test
    void shouldTakeEffectTheNextDay_whenCreatedOnASunday() {
        assertThat(createdAt("2026-11-15T10:00:00Z", WARSAW).getEffectiveWeek()).isEqualTo(LocalDate.parse("2026-11-16"));
    }

    @Test
    void shouldJudgeTheDayInTheClientsZone_whenItDiffersFromUtc() {
        // Sunday 23:30 in UTC is already Monday in Warsaw, and still Sunday afternoon in Los Angeles
        Instant sundayNight = Instant.parse("2026-11-15T23:30:00Z");

        assertThat(PlanChange.effectiveWeek(sundayNight, ZoneId.of("UTC"))).isEqualTo(LocalDate.parse("2026-11-16"));
        assertThat(PlanChange.effectiveWeek(sundayNight, WARSAW)).isEqualTo(LocalDate.parse("2026-11-23"));
        assertThat(PlanChange.effectiveWeek(sundayNight, ZoneId.of("America/Los_Angeles")))
                .isEqualTo(LocalDate.parse("2026-11-16"));
    }

    @Test
    void shouldStartPendingAndOpen_withTheDueNumbersAndPlanVersion() {
        PlanChange created = createdAt("2026-11-09T02:00:00Z", WARSAW);

        assertThat(created.getState()).isEqualTo(PlanChangeState.PENDING);
        assertThat(created.isOpen()).isTrue();
        assertThat(created.getEndedAt()).isEmpty();
        assertThat(created.getPlanVersion()).isEqualTo(3);
        assertThat(created.getConcernedExercises()).containsExactly(SQUAT);
    }

    @Test
    void shouldRefuseContentThatChangesNothing_orMovesWithoutARemovedSession() {
        assertThatThrownBy(() -> new PlanChangeContent(List.of(), Optional.empty(), List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PlanChangeContent(
                        List.of(CHANGE),
                        Optional.empty(),
                        List.of(new SessionMove(new EntryRef(DayOfWeek.FRIDAY, 0), DayOfWeek.MONDAY))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
