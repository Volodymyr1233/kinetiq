package com.kinetiq.planchange.domain.rule;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.change;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.changes;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.move;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.removal;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LowCompletionLimitsTest {

    private static final ExerciseId SQUAT = ExerciseId.random();

    private final PlanSnapshot threeSessions = plan(
            session(MONDAY, entry(SQUAT, 3, 8, "100.0")),
            session(WEDNESDAY, entry(SQUAT, 3, 8, "100.0")),
            session(FRIDAY, entry(SQUAT, 3, 8, "100.0"), entry(SQUAT, 3, 8, "90.0")));

    private final LowCompletionLimits limits = new LowCompletionLimits();

    private Set<String> violations(PlanSnapshot plan, PlanChangeContent content) {
        return limits.violations(plan, due(Rule.LOW_COMPLETION), content);
    }

    @Test
    void shouldAccept_whenOneSessionOfThreeIsRemoved_withTwoMoves() {
        assertThat(violations(
                        threeSessions,
                        removal(threeSessions, FRIDAY, move(FRIDAY, 0, MONDAY), move(FRIDAY, 1, WEDNESDAY))))
                .isEmpty();
    }

    @Test
    void shouldRefuse_whenNoSessionIsRemoved() {
        assertThat(violations(threeSessions, changes(change(threeSessions, MONDAY, 0, Target.AMOUNT, "6"))))
                .containsExactlyInAnyOrder(LimitCode.SESSION_REMOVAL_COUNT, LimitCode.TARGET_NOT_ALLOWED);
    }

    @Test
    void shouldRefuse_whenFewerThanTwoSessionsWouldRemain() {
        PlanSnapshot twoSessions = plan(
                session(MONDAY, entry(SQUAT, 3, 8, "100.0")), session(FRIDAY, entry(SQUAT, 3, 8, "100.0")));

        assertThat(violations(twoSessions, removal(twoSessions, FRIDAY))).containsExactly(LimitCode.TOO_FEW_SESSIONS);
    }
}
