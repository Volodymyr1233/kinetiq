package com.kinetiq.planchange.domain.service;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.change;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.changes;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.planchange.domain.rule.HighRpeLimits;
import com.kinetiq.planchange.domain.rule.LimitCode;
import com.kinetiq.planchange.domain.rule.LowCompletionLimits;
import com.kinetiq.planchange.domain.rule.StalledWeightLimits;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanChangeLimitsTest {

    private static final ExerciseId SQUAT = ExerciseId.random();

    private final PlanSnapshot plan =
            plan(session(MONDAY, entry(SQUAT, 3, 10, "100.0")), session(FRIDAY, entry(SQUAT, 3, 10, "100.0")));

    static PlanChangeLimits all() {
        return new PlanChangeLimits(List.of(new LowCompletionLimits(), new HighRpeLimits(), new StalledWeightLimits()));
    }

    @Test
    void shouldRefuseToStart_whenARuleHasNoLimits() {
        assertThatThrownBy(() -> new PlanChangeLimits(List.of(new HighRpeLimits(), new StalledWeightLimits())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldUseTheLimitsOfTheRule_withThePlanRules() {
        // 10 to 9 repetitions is within high RPE, but for stalled weight a load must rise
        var content = changes(change(plan, MONDAY, 0, Target.AMOUNT, "9"), change(plan, FRIDAY, 0, Target.AMOUNT, "9"));

        assertThat(all().violations(plan, due(Rule.HIGH_RPE, SQUAT), content)).isEmpty();
        assertThat(all().violations(plan, due(Rule.STALLED_WEIGHT, SQUAT), content))
                .containsExactly(LimitCode.LOAD_OUT_OF_RANGE);
        assertThat(all().violations(plan, due(Rule.LOW_COMPLETION), content))
                .containsExactlyInAnyOrder(LimitCode.SESSION_REMOVAL_COUNT, LimitCode.TARGET_NOT_ALLOWED);
    }

    @Test
    void shouldAddThePlanRules_toTheRulesLimits() {
        var content = changes(change(plan, MONDAY, 0, Target.LOAD_KG, "1150.0"));

        assertThat(all().violations(plan, due(Rule.STALLED_WEIGHT, SQUAT), content))
                .containsExactlyInAnyOrder(LimitCode.PLAN_RULE, LimitCode.LOAD_OUT_OF_RANGE);
    }

    @Test
    void shouldStopAtAnUnknownEntry_beforeTheRuleReadsIt() {
        TargetChange missing =
                new TargetChange(new EntryRef(MONDAY, 4), SQUAT, "Session 1", Target.AMOUNT, BigDecimal.TEN, BigDecimal.ONE);

        assertThat(all().violations(plan, due(Rule.HIGH_RPE, SQUAT), changes(missing)))
                .containsExactly(LimitCode.UNKNOWN_KEY);
    }
}
