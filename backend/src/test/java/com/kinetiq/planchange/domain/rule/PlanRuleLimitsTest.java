package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.change;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.changes;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.move;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.removal;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.EntrySnapshot;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanRuleLimitsTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId PLANK = ExerciseId.random();

    private final PlanSnapshot plan = plan(
            session(MONDAY, entry(SQUAT, 3, 8, "100.0"), entry(PLANK, 3, 30, null)),
            session(WEDNESDAY, entry(SQUAT, 3, 8, "100.0")),
            session(FRIDAY, entry(SQUAT, 3, 8, "90.0"), entry(PLANK, 2, 30, null)));

    private final PlanRuleLimits limits = new PlanRuleLimits();

    @Test
    void shouldAccept_whenEveryValueStaysWithinTheEntryLimits() {
        assertThat(limits.violations(plan, changes(
                        change(plan, MONDAY, 0, Target.SETS, "20"),
                        change(plan, MONDAY, 0, Target.LOAD_KG, "999.9"),
                        change(plan, MONDAY, 0, Target.RPE, "10"),
                        change(plan, MONDAY, 0, Target.REST_SECONDS, "0"))))
                .isEmpty();
    }

    @Test
    void shouldRefuse_whenAValueLeavesTheEntryLimits() {
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 0, Target.SETS, "21"))))
                .containsExactly(LimitCode.PLAN_RULE);
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 0, Target.AMOUNT, "0"))))
                .containsExactly(LimitCode.PLAN_RULE);
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 0, Target.LOAD_KG, "1000.0"))))
                .containsExactly(LimitCode.PLAN_RULE);
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 0, Target.LOAD_KG, "100.25"))))
                .containsExactly(LimitCode.PLAN_RULE);
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 0, Target.RPE, "4"))))
                .containsExactly(LimitCode.PLAN_RULE);
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 0, Target.AMOUNT, "7.5"))))
                .containsExactly(LimitCode.PLAN_RULE);
    }

    @Test
    void shouldNeverGiveALoadToAnEntryWithoutOne() {
        assertThat(limits.violations(plan, changes(change(plan, MONDAY, 1, Target.LOAD_KG, "5.0"))))
                .containsExactly(LimitCode.TARGET_NOT_ALLOWED);
    }

    @Test
    void shouldRefuseAnEntryOrSessionThatIsNotInThePlan() {
        TargetChange missing = new TargetChange(
                new EntryRef(MONDAY, 5), SQUAT, "Session 1", Target.AMOUNT, BigDecimal.TEN, BigDecimal.ONE);

        assertThat(limits.violations(plan, changes(missing))).containsExactly(LimitCode.UNKNOWN_KEY);
        assertThat(limits.violations(plan, removal(plan, SATURDAY))).containsExactly(LimitCode.UNKNOWN_KEY);
        assertThat(limits.violations(plan, removal(plan, FRIDAY, move(FRIDAY, 0, SATURDAY))))
                .containsExactly(LimitCode.UNKNOWN_KEY);
        assertThat(limits.violations(plan, removal(plan, FRIDAY, move(FRIDAY, 3, MONDAY))))
                .containsExactly(LimitCode.UNKNOWN_KEY);
    }

    @Test
    void shouldRefuseTheSameTargetTwice() {
        assertThat(limits.violations(plan, changes(
                        change(plan, MONDAY, 0, Target.AMOUNT, "7"), change(plan, MONDAY, 0, Target.AMOUNT, "6"))))
                .containsExactly(LimitCode.PLAN_RULE);
    }

    @Test
    void shouldKeepAtLeastTwoSessions() {
        PlanSnapshot two = plan(session(MONDAY, entry(SQUAT, 3, 8, "100.0")), session(FRIDAY));

        assertThat(limits.violations(two, removal(two, FRIDAY))).containsExactly(LimitCode.TOO_FEW_SESSIONS);
        assertThat(limits.violations(plan, removal(plan, FRIDAY))).isEmpty();
    }

    @Test
    void shouldRefuseMoreThan8Entries_inASessionThatReceivesMoves() {
        EntrySnapshot squat = entry(SQUAT, 3, 8, "100.0");
        PlanSnapshot full = plan(
                session(MONDAY, Collections.nCopies(7, squat).toArray(EntrySnapshot[]::new)),
                session(WEDNESDAY, squat),
                session(FRIDAY, squat, squat));

        assertThat(limits.violations(full, removal(full, FRIDAY, move(FRIDAY, 0, MONDAY)))).isEmpty();
        assertThat(limits.violations(full, removal(full, FRIDAY, move(FRIDAY, 0, MONDAY), move(FRIDAY, 1, MONDAY))))
                .containsExactly(LimitCode.TOO_MANY_ENTRIES);
    }

    @Test
    void shouldLeaveAnAlreadyLongSessionAlone_whenItReceivesNothing() {
        EntrySnapshot squat = entry(SQUAT, 3, 8, "100.0");
        PlanSnapshot longSession = plan(
                session(MONDAY, Collections.nCopies(10, squat).toArray(EntrySnapshot[]::new)), session(FRIDAY, squat));

        assertThat(limits.violations(longSession, changes(change(longSession, MONDAY, 0, Target.AMOUNT, "7")))).isEmpty();
    }

    @Test
    void shouldApplyTheContent_toTheResultingPlan() {
        PlanSnapshot result = plan.apply(new PlanChangeContent(
                List.of(change(plan, MONDAY, 0, Target.AMOUNT, "7")),
                removal(plan, FRIDAY).removed(),
                List.of(move(FRIDAY, 1, WEDNESDAY))));

        assertThat(result.sessions()).extracting(SessionSnapshot::day).containsExactly(MONDAY, WEDNESDAY);
        assertThat(result.entry(new EntryRef(MONDAY, 0)).orElseThrow().amount()).isEqualTo(7);
        assertThat(result.session(WEDNESDAY).orElseThrow().entries())
                .extracting(EntrySnapshot::exercise)
                .containsExactly(SQUAT, PLANK);
    }
}
