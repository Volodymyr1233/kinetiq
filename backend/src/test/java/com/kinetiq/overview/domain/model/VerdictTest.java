package com.kinetiq.overview.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.client.domain.model.PlanState;
import java.math.BigDecimal;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class VerdictTest {

    private static Verdict running(Completion completion, String averageRpe) {
        return Verdict.of(
                PlanState.PUBLISHED, false, true, completion, averageRpe == null ? null : new BigDecimal(averageRpe));
    }

    @Nested
    class Completed {

        // 20 due sessions put every whole percent on a session count: 11 is 55%, 12 is 60%, 13 is 65%, 14 is 70%
        @ParameterizedTest
        @CsvSource({"11, AT_RISK", "12, WATCH", "13, WATCH", "14, ON_TRACK"})
        void shouldFollowTheThresholds_whenTheShareDoneCrossesThem(int done, ClientStatus expected) {
            assertThat(running(new Completion(done, 20), null).status()).isEqualTo(expected);
        }

        @Test
        void shouldBeAtRisk_whenJustBelow60PercentWithoutRounding() {
            // 59.9%, which rounds to 60%
            Verdict verdict = running(new Completion(599, 1000), null);

            assertThat(verdict.status()).isEqualTo(ClientStatus.AT_RISK);
            assertThat(verdict.reasons()).containsExactly(StatusReason.LOW_COMPLETION);
        }

        @Test
        void shouldBeWatch_whenJustBelow70PercentWithoutRounding() {
            Verdict verdict = running(new Completion(699, 1000), null);

            assertThat(verdict.status()).isEqualTo(ClientStatus.WATCH);
            assertThat(verdict.reasons()).containsExactly(StatusReason.LOW_COMPLETION);
        }

        @Test
        void shouldBeAtRisk_whenTheOnlyDueSessionWasMissed() {
            assertThat(running(new Completion(0, 1), null).status()).isEqualTo(ClientStatus.AT_RISK);
        }
    }

    @Nested
    class Rpe {

        @Test
        void shouldBeWatch_whenTheAverageIsAbove8() {
            Verdict verdict = running(new Completion(10, 10), "8.1");

            assertThat(verdict.status()).isEqualTo(ClientStatus.WATCH);
            assertThat(verdict.reasons()).containsExactly(StatusReason.HIGH_RPE);
        }

        @Test
        void shouldBeOnTrack_whenTheAverageIs8() {
            Verdict verdict = running(new Completion(10, 10), "8.0");

            assertThat(verdict.status()).isEqualTo(ClientStatus.ON_TRACK);
            assertThat(verdict.reasons()).isEmpty();
        }

        @Test
        void shouldKeepAtRiskWithBothReasons_whenAlsoAbove8() {
            Verdict verdict = running(new Completion(5, 10), "8.6");

            assertThat(verdict.status()).isEqualTo(ClientStatus.AT_RISK);
            assertThat(verdict.reasons()).containsExactlyInAnyOrder(StatusReason.LOW_COMPLETION, StatusReason.HIGH_RPE);
        }

        @Test
        void shouldGiveBothReasons_whenWatchedForCompletionAndRpe() {
            assertThat(running(new Completion(13, 20), "9.0").reasons())
                    .containsExactlyInAnyOrder(StatusReason.LOW_COMPLETION, StatusReason.HIGH_RPE);
        }

        @Test
        void shouldDecideByRpeAlone_whenNoSessionIsDueYet() {
            assertThat(running(null, "8.1").status()).isEqualTo(ClientStatus.WATCH);
            assertThat(running(null, null).status()).isEqualTo(ClientStatus.ON_TRACK);
        }
    }

    @Nested
    class WithoutARunningPlan {

        @ParameterizedTest
        @EnumSource(
                value = PlanState.class,
                names = {"PLAN_TO_REVIEW", "DRAFT", "DRAFT_FAILED"})
        void shouldBePlanToReview_whenTheTrainerHasAPlanToReview(PlanState state) {
            assertThat(Verdict.of(state, false, false, null, null).status()).isEqualTo(ClientStatus.PLAN_TO_REVIEW);
        }

        @ParameterizedTest
        @EnumSource(
                value = PlanState.class,
                names = {"NO_QUIZ", "PLAN_DRAFTING", "PUBLISHED"})
        void shouldBeNoPlan_whenNothingWaitsForTheTrainer(PlanState state) {
            // a published plan that is not running has finished
            assertThat(Verdict.of(state, false, false, null, null).status()).isEqualTo(ClientStatus.NO_PLAN);
        }

        @Test
        void shouldBePlanToReview_whenAStoredDraftIsBeingBuiltAgain() {
            assertThat(Verdict.of(PlanState.PLAN_DRAFTING, true, false, null, null).status())
                    .isEqualTo(ClientStatus.PLAN_TO_REVIEW);
        }

        @Test
        void shouldIgnoreRpe_whenThePlanHasFinished() {
            Verdict verdict = Verdict.of(PlanState.PUBLISHED, false, false, null, new BigDecimal("9.5"));

            assertThat(verdict.status()).isEqualTo(ClientStatus.NO_PLAN);
            assertThat(verdict.reasons()).isEmpty();
        }
    }
}
