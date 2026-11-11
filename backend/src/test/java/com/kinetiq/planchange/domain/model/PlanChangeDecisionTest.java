package com.kinetiq.planchange.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.InvalidValueException;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import com.kinetiq.planchange.domain.exception.PlanChangeNothingToUndoException;
import com.kinetiq.planchange.domain.exception.PlanChangeWeekBegunException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanChangeDecisionTest {

    private static final LocalDate EFFECTIVE = LocalDate.parse("2026-11-16");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");
    private static final Instant FIRST = Instant.parse("2026-11-12T08:00:00Z");
    private static final Instant SECOND = Instant.parse("2026-11-12T09:00:00Z");
    private static final ExerciseId SQUAT = ExerciseId.random();

    private static PlanChange pending() {
        return new PlanChange(
                PlanChangeId.random(),
                AccountId.random(),
                Rule.HIGH_RPE,
                PlanChangeState.PENDING,
                3,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                EFFECTIVE,
                PlanChangeContent.ofChanges(List.of(new TargetChange(
                        new EntryRef(DayOfWeek.MONDAY, 0),
                        SQUAT,
                        "Lower body",
                        Target.AMOUNT,
                        new BigDecimal("10.0"),
                        new BigDecimal("9.0")))),
                List.of(),
                Set.of(SQUAT),
                Instant.parse("2026-11-09T02:00:00Z"),
                null,
                null,
                null,
                null);
    }

    private static PlanChange acceptedBy(Side side) {
        return pending().accept(side, FIRST, TODAY);
    }

    private static Side other(Side side) {
        return side == Side.TRAINER ? Side.CLIENT : Side.TRAINER;
    }

    @Nested
    class Accept {

        @Test
        void shouldBecomeAcceptedByOne_whenEitherSideAcceptsAPendingOne() {
            for (Side side : Side.values()) {
                PlanChange accepted = acceptedBy(side);

                assertThat(accepted.getState()).isEqualTo(PlanChangeState.ACCEPTED_BY_ONE);
                assertThat(accepted.acceptedBy(side)).isTrue();
                assertThat(accepted.acceptedBy(other(side))).isFalse();
                assertThat(accepted.isOpen()).isTrue();
                assertThat(accepted.getEndedAt()).isEmpty();
            }
        }

        @Test
        void shouldBeApplied_whenTheOtherSideAcceptsToo() {
            for (Side side : Side.values()) {
                PlanChange applied = acceptedBy(side).accept(other(side), SECOND, TODAY);

                assertThat(applied.getState()).isEqualTo(PlanChangeState.APPLIED);
                assertThat(applied.acceptedBy(Side.TRAINER)).isTrue();
                assertThat(applied.acceptedBy(Side.CLIENT)).isTrue();
                assertThat(applied.isOpen()).isFalse();
                assertThat(applied.getEndedAt()).contains(SECOND);
            }
        }

        @Test
        void shouldChangeNothing_whenTheSameSideAcceptsTwice() {
            PlanChange accepted = acceptedBy(Side.CLIENT);

            assertThat(accepted.accept(Side.CLIENT, SECOND, TODAY)).isSameAs(accepted);
            assertThat(accepted.getClientAcceptedAt()).contains(FIRST);
        }

        @Test
        void shouldBeRefused_whenItEnded() {
            PlanChange applied = acceptedBy(Side.TRAINER).accept(Side.CLIENT, SECOND, TODAY);
            PlanChange declined = pending().decline(Side.CLIENT, DeclineReason.OTHER, null, FIRST, TODAY);

            for (PlanChange ended : List.of(applied, declined)) {
                for (Side side : Side.values()) {
                    assertThatThrownBy(() -> ended.accept(side, SECOND, TODAY))
                            .isInstanceOf(PlanChangeEndedException.class);
                }
            }
        }

        @Test
        void shouldBeAllowedOnTheSundayBeforeTheEffectiveWeek_andRefusedFromItsMonday() {
            assertThat(pending().accept(Side.CLIENT, FIRST, EFFECTIVE.minusDays(1)).getState())
                    .isEqualTo(PlanChangeState.ACCEPTED_BY_ONE);

            assertThatThrownBy(() -> pending().accept(Side.CLIENT, FIRST, EFFECTIVE))
                    .isInstanceOf(PlanChangeWeekBegunException.class);
            assertThatThrownBy(() -> pending().accept(Side.CLIENT, FIRST, EFFECTIVE.plusDays(3)))
                    .isInstanceOf(PlanChangeWeekBegunException.class);
        }
    }

    @Nested
    class Undo {

        @Test
        void shouldBePendingAgainWithNobodysAcceptance_whenTheAcceptorUndoes() {
            for (Side side : Side.values()) {
                PlanChange undone = acceptedBy(side).undo(side, TODAY);

                assertThat(undone.getState()).isEqualTo(PlanChangeState.PENDING);
                assertThat(undone.acceptedBy(Side.TRAINER)).isFalse();
                assertThat(undone.acceptedBy(Side.CLIENT)).isFalse();
            }
        }

        @Test
        void shouldBeRefused_whenTheOtherSideAcceptedNotTheOneUndoing() {
            assertThatThrownBy(() -> acceptedBy(Side.CLIENT).undo(Side.TRAINER, TODAY))
                    .isInstanceOf(PlanChangeNothingToUndoException.class);
        }

        @Test
        void shouldBeRefused_whenNobodyAcceptedYet() {
            assertThatThrownBy(() -> pending().undo(Side.CLIENT, TODAY)).isInstanceOf(PlanChangeNothingToUndoException.class);
        }

        @Test
        void shouldBeRefusedAsEnded_whenItIsApplied_forEitherSide() {
            PlanChange applied = acceptedBy(Side.TRAINER).accept(Side.CLIENT, SECOND, TODAY);

            for (Side side : Side.values()) {
                assertThatThrownBy(() -> applied.undo(side, TODAY)).isInstanceOf(PlanChangeEndedException.class);
            }
        }

        @Test
        void shouldBeRefused_whenTheEffectiveWeekHasBegun() {
            PlanChange accepted = acceptedBy(Side.TRAINER);

            assertThatThrownBy(() -> accepted.undo(Side.TRAINER, EFFECTIVE))
                    .isInstanceOf(PlanChangeWeekBegunException.class);
        }

        @Test
        void shouldBeRefusedAsEnded_whenItIsDeclined() {
            PlanChange declined = pending().decline(Side.TRAINER, DeclineReason.INJURY, null, FIRST, TODAY);

            assertThatThrownBy(() -> declined.undo(Side.TRAINER, TODAY)).isInstanceOf(PlanChangeEndedException.class);
        }
    }

    @Nested
    class Decline {

        @Test
        void shouldEndAsDeclined_fromPendingOrAcceptedByOne_withWhoWhyAndTheNote() {
            for (PlanChange open : List.of(pending(), acceptedBy(Side.CLIENT))) {
                PlanChange declined =
                        open.decline(Side.TRAINER, DeclineReason.SCHEDULE, "  Away that week \n", SECOND, TODAY);

                assertThat(declined.getState()).isEqualTo(PlanChangeState.DECLINED);
                assertThat(declined.isOpen()).isFalse();
                assertThat(declined.getEndedAt()).contains(SECOND);
                assertThat(declined.getDecision())
                        .contains(new Decision(Side.TRAINER, DeclineReason.SCHEDULE, "Away that week"));
            }
        }

        @Test
        void shouldKeepTheAcceptanceAsHistory_whenDeclinedAfterOneAccepted() {
            PlanChange declined =
                    acceptedBy(Side.CLIENT).decline(Side.TRAINER, DeclineReason.TOO_HARD, null, SECOND, TODAY);

            assertThat(declined.acceptedBy(Side.CLIENT)).isTrue();
        }

        @Test
        void shouldBeRefused_whenItEndedOrItsWeekBegan() {
            PlanChange applied = acceptedBy(Side.TRAINER).accept(Side.CLIENT, SECOND, TODAY);

            assertThatThrownBy(() -> applied.decline(Side.CLIENT, DeclineReason.OTHER, null, SECOND, TODAY))
                    .isInstanceOf(PlanChangeEndedException.class);
            assertThatThrownBy(() -> pending().decline(Side.CLIENT, DeclineReason.OTHER, null, SECOND, EFFECTIVE))
                    .isInstanceOf(PlanChangeWeekBegunException.class);
        }

        @Test
        void shouldTurnABlankNoteIntoNone_andRefuseOneOver500Characters() {
            assertThat(new Decision(Side.CLIENT, DeclineReason.OTHER, " \n ").note()).isNull();
            assertThat(new Decision(Side.CLIENT, DeclineReason.OTHER, "x".repeat(500)).note()).hasSize(500);
            assertThatThrownBy(() -> new Decision(Side.CLIENT, DeclineReason.OTHER, "x".repeat(501)))
                    .isInstanceOf(InvalidValueException.class);
        }
    }
}
