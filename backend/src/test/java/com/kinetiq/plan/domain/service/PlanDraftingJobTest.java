package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.plan.domain.exception.PlanGenerationFailedException;
import com.kinetiq.plan.domain.exception.PlanGeneratorUnavailableException;
import com.kinetiq.plan.domain.model.AutomaticDraft;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

class PlanDraftingJobTest {

    private final DraftingFixture fixture = new DraftingFixture();
    private final StubPlanGenerator generator = new StubPlanGenerator();
    private final PlanDraftingService service =
            new PlanDraftingService(fixture.quiz, fixture.accounts, fixture.exercises, fixture.plans);
    private final PlanDraftingJob job = new PlanDraftingJob(service, generator, Duration.ZERO);

    private Plan stored() {
        return fixture.plans.byClient.get(fixture.client);
    }

    private void assertEmptyFailedDraft() {
        assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.FAILED);
        assertThat(stored().getSessions()).isEmpty();
        assertThat(stored().getGoal()).isEqualTo(Goal.LOSE_FAT);
        assertThat(stored().getWeeks()).isEqualTo(12);
    }

    @Test
    void shouldStoreTheDraftAfterOneAttempt_whenFirstAnswerIsValid() {
        generator.answering(DraftingFixture.validAnswer());

        job.run(fixture.client);

        assertThat(generator.received).hasSize(1);
        assertThat(generator.received.getFirst().rejections()).isEmpty();
        assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.BUILT);
        assertThat(stored().getSessions()).hasSize(2);
    }

    @Test
    void shouldNameTheReasonsInTheSecondPrompt_whenFirstAnswerIsRejected() {
        generator.answering(DraftingFixture.answerWithUnknownExercise()).answering(DraftingFixture.validAnswer());

        job.run(fixture.client);

        assertThat(generator.received).hasSize(2);
        assertThat(generator.received.getLast().rejections())
                .containsExactly("Every exercise must be a key from the CATALOGUE.");
        assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.BUILT);
    }

    @Test
    void shouldStoreAnEmptyFailedDraft_whenBothAnswersAreRejected() {
        generator
                .answering(DraftingFixture.answerWithUnknownExercise())
                .answering(DraftingFixture.answerWithUnknownExercise());

        job.run(fixture.client);

        assertThat(generator.received).hasSize(2);
        assertEmptyFailedDraft();
    }

    @Test
    void shouldRepeatTheSamePrompt_whenFirstAttemptFailsWithoutAnAnswer() {
        generator.failing(new PlanGenerationFailedException("timed out")).answering(DraftingFixture.validAnswer());

        job.run(fixture.client);

        assertThat(generator.received).hasSize(2);
        assertThat(generator.received.getLast()).isEqualTo(generator.received.getFirst());
        assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.BUILT);
    }

    @Test
    void shouldStoreAnEmptyFailedDraft_whenBothAttemptsFail() {
        generator
                .failing(new PlanGenerationFailedException("timed out"))
                .failing(new PlanGenerationFailedException("too many requests"));

        job.run(fixture.client);

        assertThat(generator.received).hasSize(2);
        assertEmptyFailedDraft();
    }

    @Test
    void shouldStoreAtOnceWithoutAnotherAttempt_whenNoGeneratorIsConfigured() {
        generator.failing(new PlanGeneratorUnavailableException());
        PlanDraftingJob slowPause = new PlanDraftingJob(service, generator, Duration.ofHours(1));

        slowPause.run(fixture.client);

        assertThat(generator.received).hasSize(1);
        assertEmptyFailedDraft();
    }

    @Test
    void shouldLeaveAPlanThatIsThereAlone_whenAnAnswerArrives() {
        Plan trainers = fixture.plans.save(Plan.draft(fixture.client, Goal.GET_STRONGER, 8, List.of()));
        generator.answering(DraftingFixture.validAnswer());

        job.run(fixture.client);

        assertThat(stored().getId()).isEqualTo(trainers.getId());
        assertThat(stored().getAutomaticDraft()).isEmpty();
        assertThat(stored().getGoal()).isEqualTo(Goal.GET_STRONGER);
    }

    @Test
    void shouldStoreNothingAndAskNothing_whenAnswersAreGone() {
        fixture.answered = false;

        job.run(fixture.client);

        assertThat(generator.received).isEmpty();
        assertThat(stored()).isNull();
    }

    @Test
    void shouldNotThrow_whenSomethingUnexpectedHappens() {
        generator.failing(new IllegalStateException("unexpected"));

        job.run(fixture.client);

        assertThat(stored()).isNull();
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldLogNoClientTextOrId_whenAttemptsAreRejectedAndEndInFailure(CapturedOutput log) {
        generator
                .answering(DraftingFixture.answerWithUnknownExercise())
                .failing(new PlanGenerationFailedException("timed out"));

        job.run(fixture.client);

        assertThat(log.getAll())
                .contains("draft.unknown-exercise", "outcome=failed")
                .doesNotContain(DraftingFixture.NOTES, "SENTINEL")
                .doesNotContain(fixture.client.value().toString(), fixture.trainer.value().toString())
                .doesNotContain("Zdzis", "Qwerty", "Bogumi", "Asdfg");
    }
}
