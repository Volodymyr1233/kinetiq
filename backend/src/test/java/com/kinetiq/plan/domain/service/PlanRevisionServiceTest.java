package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanRevisionServiceTest {

    private static final AccountId CLIENT = AccountId.random();
    private static final LocalDate FIRST_WEEK = LocalDate.parse("2026-09-28");
    private static final LocalDate EFFECTIVE_WEEK = LocalDate.parse("2026-11-16");
    private static final ExerciseId SQUAT = ExerciseId.random();

    private final InMemoryPlanRepository plans = new InMemoryPlanRepository();
    private final PlanRevisionService service = new PlanRevisionService(plans);

    private static Session session(DayOfWeek day, int sets) {
        return new Session(
                day.name(), day, List.of(new ExerciseEntry(SQUAT, sets, 8, new BigDecimal("60.0"), 8, 90)));
    }

    private static List<Session> sessions(int sets) {
        return List.of(session(DayOfWeek.MONDAY, sets), session(DayOfWeek.THURSDAY, sets));
    }

    private Plan publishedPlan() {
        Plan draft = Plan.draft(CLIENT, Goal.BUILD_MUSCLE, 8, sessions(3));
        return plans.save(plans.save(draft).publish(FIRST_WEEK));
    }

    @Test
    void shouldKeepOldSessionsInRevisionAndSaveNewOnes_whenRevised() {
        Plan stored = publishedPlan();

        service.revise(CLIENT, stored.getVersion(), EFFECTIVE_WEEK, sessions(4));

        Plan live = plans.findByClient(CLIENT).orElseThrow();
        assertThat(live.getSessions()).isEqualTo(sessions(4));
        assertThat(plans.revisionsOf(stored.getId()))
                .singleElement()
                .satisfies(revision -> {
                    assertThat(revision.validUntil()).isEqualTo(EFFECTIVE_WEEK);
                    assertThat(revision.sessions()).isEqualTo(sessions(3));
                });
    }

    @Test
    void shouldRaiseVersion_whenRevised() {
        Plan stored = publishedPlan();

        service.revise(CLIENT, stored.getVersion(), EFFECTIVE_WEEK, sessions(4));

        assertThat(plans.findByClient(CLIENT).orElseThrow().getVersion()).isGreaterThan(stored.getVersion());
    }

    @Test
    void shouldRefuseAndStoreNothing_whenPlanWasSavedAfterTheCallerReadIt() {
        Plan stored = publishedPlan();

        assertThatThrownBy(() -> service.revise(CLIENT, stored.getVersion() - 1, EFFECTIVE_WEEK, sessions(4)))
                .isInstanceOf(PlanChangedException.class);

        assertThat(plans.revisions).isEmpty();
        assertThat(plans.findByClient(CLIENT).orElseThrow().getSessions()).isEqualTo(sessions(3));
    }

    @Test
    void shouldRefuseAndStoreNothing_whenResultBreaksAPlanRule() {
        Plan stored = publishedPlan();

        assertThatThrownBy(() ->
                        service.revise(CLIENT, stored.getVersion(), EFFECTIVE_WEEK, List.of(session(DayOfWeek.MONDAY, 4))))
                .isInstanceOf(PlanRuleViolationException.class);

        assertThat(plans.revisions).isEmpty();
        assertThat(plans.findByClient(CLIENT).orElseThrow().getSessions()).isEqualTo(sessions(3));
    }

    @Test
    void shouldFindNoPlan_whenClientHasNone() {
        assertThatThrownBy(() -> service.revise(CLIENT, 0, EFFECTIVE_WEEK, sessions(4)))
                .isInstanceOf(PlanNotFoundException.class);
    }

    @Test
    void shouldFindNoPlan_whenPlanIsADraft() {
        Plan draft = plans.save(Plan.draft(CLIENT, Goal.BUILD_MUSCLE, 8, sessions(3)));

        assertThatThrownBy(() -> service.revise(CLIENT, draft.getVersion(), EFFECTIVE_WEEK, sessions(4)))
                .isInstanceOf(PlanNotFoundException.class);
        assertThat(plans.revisions).isEmpty();
    }
}
