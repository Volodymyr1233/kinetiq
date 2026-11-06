package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.plan.domain.exception.GeneratedPlanRejectedException;
import com.kinetiq.plan.domain.input.DraftInput;
import com.kinetiq.plan.domain.model.AutomaticDraft;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanDraftingServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-05T10:00:00Z");
    private static final Duration LIMIT = Duration.ofMinutes(5).plusSeconds(30);

    private final DraftingFixture fixture = new DraftingFixture();
    private final PlanDraftingService service = serviceAt(NOW);

    private PlanDraftingService serviceAt(Instant now) {
        return new PlanDraftingService(
                fixture.quiz,
                fixture.accounts,
                fixture.exercises,
                fixture.plans,
                fixture.draftings,
                Clock.fixed(now, ZoneOffset.UTC),
                LIMIT);
    }

    @BeforeEach
    void requestDrafting() {
        service.request(fixture.client);
    }

    private List<Session> sessions() {
        return sessions(fixture.library.getFirst().getId());
    }

    private static List<Session> sessions(ExerciseId exercise) {
        ExerciseEntry entry = new ExerciseEntry(exercise, 3, 10, null, 7, 90);
        return List.of(
                new Session("Lower body", DayOfWeek.MONDAY, List.of(entry)),
                new Session("Upper body", DayOfWeek.THURSDAY, List.of(entry)));
    }

    private Plan stored() {
        return fixture.plans.byClient.get(fixture.client);
    }

    @Nested
    class Input {

        @Test
        void shouldHoldEverythingAPromptNeeds_whenTheClientHasAnswersAndATrainer() {
            DraftInput input = service.input(fixture.client).orElseThrow();

            assertThat(input.answers().getGoal()).isEqualTo(Goal.LOSE_FAT);
            assertThat(input.trainer()).isEqualTo(fixture.trainer);
            assertThat(input.clientName()).isEqualTo(DraftingFixture.CLIENT_NAME);
            assertThat(input.trainerName()).isEqualTo(DraftingFixture.TRAINER_NAME);
            assertThat(input.language()).isEqualTo(Language.PL);
            assertThat(input.library()).hasSameElementsAs(fixture.library);
        }

        @Test
        void shouldBeEmpty_whenAnswersAreGone() {
            fixture.answered = false;

            assertThat(service.input(fixture.client)).isEmpty();
        }

        @Test
        void shouldBeEmpty_whenTheTrainerIsGone() {
            fixture.clientsTrainer = null;

            assertThat(service.input(fixture.client)).isEmpty();
        }
    }

    @Nested
    class StoreBuilt {

        @Test
        void shouldStoreAMarkedDraftWithTheQuizGoalAndTwelveWeeks() {
            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored().isPublished()).isFalse();
            assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.BUILT);
            assertThat(stored().getGoal()).isEqualTo(Goal.LOSE_FAT);
            assertThat(stored().getWeeks()).isEqualTo(12);
            assertThat(stored().getSessions()).isEqualTo(sessions());
        }

        @Test
        void shouldEndTheDrafting_whenStored() {
            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(fixture.draftings.rows).isEmpty();
        }

        @Test
        void shouldDropTheResult_whenTheDraftingHasEndedAlready() {
            fixture.draftings.remove(fixture.client);

            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored()).isNull();
        }

        @Test
        void shouldRefuse_whenAnExerciseHasLeftTheLibrary() {
            assertThatThrownBy(() -> service.storeBuilt(fixture.client, fixture.trainer, sessions(ExerciseId.random())))
                    .isInstanceOf(GeneratedPlanRejectedException.class)
                    .satisfies(thrown -> assertThat(((GeneratedPlanRejectedException) thrown).codes())
                            .containsExactly("draft.unknown-exercise"));
            assertThat(stored()).isNull();
        }

        @Test
        void shouldStoreNothing_whenTheClientsTrainerHasChanged() {
            fixture.clientsTrainer = AccountId.random();

            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored()).isNull();
        }

        @Test
        void shouldStoreNothing_whenTheTrainerIsGone() {
            fixture.clientsTrainer = null;

            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored()).isNull();
        }

        @Test
        void shouldStoreNothing_whenAnswersAreGone() {
            fixture.answered = false;

            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored()).isNull();
        }

        @Test
        void shouldLeaveThePlanAsItIs_whenTheClientHasOne() {
            Plan trainers = fixture.plans.save(Plan.draft(fixture.client, Goal.GET_STRONGER, 8, List.of()));

            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored()).isSameAs(trainers);
            assertThat(fixture.draftings.rows).isEmpty();
        }
    }

    @Nested
    class StoreFailed {

        @Test
        void shouldStoreAnEmptyMarkedDraftWithTheQuizGoalAndTwelveWeeks() {
            service.storeFailed(fixture.client);

            assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.FAILED);
            assertThat(stored().getSessions()).isEmpty();
            assertThat(stored().getGoal()).isEqualTo(Goal.LOSE_FAT);
            assertThat(stored().getWeeks()).isEqualTo(12);
        }

        @Test
        void shouldStoreNothingAndNotThrow_whenAnswersAreGone() {
            fixture.answered = false;

            service.storeFailed(fixture.client);

            assertThat(stored()).isNull();
        }

        @Test
        void shouldStoreNothingAndNotThrow_whenTheTrainerIsGone() {
            fixture.clientsTrainer = null;

            service.storeFailed(fixture.client);

            assertThat(stored()).isNull();
        }

        @Test
        void shouldLeaveThePlanAsItIs_whenTheClientHasOne() {
            Plan trainers = fixture.plans.save(Plan.draft(fixture.client, Goal.GET_STRONGER, 8, List.of()));

            service.storeFailed(fixture.client);

            assertThat(stored()).isSameAs(trainers);
            assertThat(fixture.draftings.rows).isEmpty();
        }

        @Test
        void shouldStoreOnlyOnce_whenTheJobAndTheSweepBothEndADrafting() {
            service.storeFailed(fixture.client);
            Plan first = stored();

            service.storeFailed(fixture.client);
            service.storeBuilt(fixture.client, fixture.trainer, sessions());

            assertThat(stored()).isSameAs(first);
        }
    }

    @Nested
    class Overdue {


        @Test
        void shouldLeaveARow_atTheLimit() {
            assertThat(serviceAt(NOW.plus(LIMIT)).overdue()).isEmpty();
        }

        @Test
        void shouldFindARow_pastTheLimit() {
            assertThat(serviceAt(NOW.plus(LIMIT).plusSeconds(1)).overdue()).containsExactly(fixture.client);
        }

        @Test
        void shouldFindNothing_onceTheDraftingHasEnded() {
            service.storeFailed(fixture.client);

            assertThat(serviceAt(NOW.plus(LIMIT).plusSeconds(1)).overdue()).isEmpty();
        }

        @Test
        void shouldEndWithinSixMinutes_whenARestartLeftADrafting() {
            PlanDraftingService later = serviceAt(NOW.plus(LIMIT).plusSeconds(30));

            later.overdue().forEach(later::storeFailed);

            assertThat(stored().getAutomaticDraft()).contains(AutomaticDraft.FAILED);
            assertThat(fixture.draftings.rows).isEmpty();
        }
    }
}
