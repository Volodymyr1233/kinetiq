package com.kinetiq.planchange.domain.service;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.due;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.plan;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.session;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.exception.PlanChangeDraftingFailedException;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.ProposedContent;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PlanChangeDraftingServiceTest {

    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final ExerciseId ROW = ExerciseId.random();

    // high RPE on the squat: the fixed transformation gives 3 x 9, so a kept answer is told apart by 2 x 14 (93%)
    private final PlanSnapshot plan =
            plan(session(MONDAY, entry(SQUAT, 3, 10, "100.0"), entry(ROW, 3, 10, "50.0")), session(FRIDAY, entry(ROW, 3, 10, "50.0")));
    private final DuePlanChange due = due(Rule.HIGH_RPE, SQUAT);

    private final FakePlanChangeDrafter drafter = new FakePlanChangeDrafter();

    private static ProposedContent squat(int sets, int amount) {
        return new ProposedContent(
                List.of(new ProposedContent.Change("s1e1", sets, amount, null)), Optional.empty(), List.of());
    }

    private static final ProposedContent VALID = squat(2, 14);

    private List<BigDecimal> newValues(Optional<PlanChangeContent> content) {
        return content.orElseThrow().changes().stream().map(TargetChange::newValue).toList();
    }

    @Test
    void shouldAskOnce_whenTheFirstAnswerKeepsTheLimits() {
        Optional<PlanChangeContent> content = drafter.answering(VALID).drafting().draft(plan, due);

        assertThat(drafter.asked).hasSize(1);
        assertThat(newValues(content)).containsExactly(BigDecimal.valueOf(2), BigDecimal.valueOf(14));
    }

    @Test
    void shouldUseTheSecondAnswer_andSayWhyTheFirstWasRefused() {
        // 3 x 7 is 70% of the volume, below the 85% limit
        Optional<PlanChangeContent> content =
                drafter.answering(squat(3, 7)).answering(VALID).drafting().draft(plan, due);

        assertThat(drafter.asked).hasSize(2);
        assertThat(drafter.asked.getFirst().rejections()).isEmpty();
        assertThat(drafter.asked.getLast().rejections()).containsExactly(
                "For each changed entry, sets x amount must end at 85% to 95% of what it was.");
        assertThat(newValues(content)).containsExactly(BigDecimal.valueOf(2), BigDecimal.valueOf(14));
    }

    @Test
    void shouldUseTheFixedTransformation_whenBothAnswersBreakTheLimits() {
        Optional<PlanChangeContent> content =
                drafter.answering(squat(3, 7)).answering(squat(1, 10)).drafting().draft(plan, due);

        assertThat(drafter.asked).hasSize(2);
        // 10 x 0.9 = 9 repetitions on the squat only
        assertThat(content.orElseThrow().changes()).hasSize(1);
        assertThat(newValues(content)).containsExactly(BigDecimal.valueOf(9));
    }

    @Test
    void shouldCountAFailedOrUnreadableAnswerAsAnAttempt() {
        Optional<PlanChangeContent> content = drafter.failing(new PlanChangeDraftingFailedException("cut off"))
                .answering(new ProposedContent(List.of(), Optional.of("s9"), List.of()))
                .drafting()
                .draft(plan, due);

        assertThat(drafter.asked).hasSize(2);
        assertThat(newValues(content)).containsExactly(BigDecimal.valueOf(9));
    }

    @Test
    void shouldUseTheFixedTransformationAtOnce_whenNoServiceIsConfigured() {
        Optional<PlanChangeContent> content = drafter.drafting().draft(plan, due);

        assertThat(drafter.asked).hasSize(1);
        assertThat(newValues(content)).containsExactly(BigDecimal.valueOf(9));
    }

    @Test
    void shouldRefuseAnAnswerThatChangesAnotherExercise() {
        ProposedContent rowToo = new ProposedContent(
                List.of(
                        new ProposedContent.Change("s1e1", 2, 14, null),
                        new ProposedContent.Change("s1e2", null, 9, null)),
                Optional.empty(),
                List.of());

        Optional<PlanChangeContent> content = drafter.answering(rowToo).answering(rowToo).drafting().draft(plan, due);

        assertThat(drafter.asked.getLast().rejections()).containsExactly("Change only entries with concerned=true.");
        assertThat(content.orElseThrow().changes()).extracting(TargetChange::exercise).containsOnly(SQUAT);
        assertThat(newValues(content)).containsExactly(BigDecimal.valueOf(9));
    }

    @Test
    void shouldGiveNothing_whenNoAnswerFitsAndTheFormulaChangesNothing() {
        PlanSnapshot single = plan(session(MONDAY, entry(SQUAT, 3, 1, "100.0")), session(FRIDAY));

        assertThat(drafter.drafting().draft(single, due)).isEmpty();
    }
}
