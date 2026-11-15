package com.kinetiq.planchange.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.exception.PlanChangeEndedException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class PlanChangeSettlementTest {

    private static final LocalDate EFFECTIVE = LocalDate.parse("2026-11-16");
    private static final LocalDate TODAY = LocalDate.parse("2026-11-12");
    private static final Instant ACCEPTED_AT = Instant.parse("2026-11-12T09:00:00Z");
    private static final Instant MONDAY = Instant.parse("2026-11-15T23:05:00Z");
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

    private static PlanChange upcoming() {
        return pending()
                .accept(Side.TRAINER, ACCEPTED_AT.minusSeconds(60), TODAY)
                .accept(Side.CLIENT, ACCEPTED_AT, TODAY);
    }

    @Nested
    class TakeEffect {

        @Test
        void shouldBeInEffectAndKeepWhenItWasApplied_whenAnUpcomingOneTakesEffect() {
            PlanChange upcoming = upcoming();
            assertThat(upcoming.isUpcoming()).isTrue();
            assertThat(upcoming.isInEffect()).isFalse();

            PlanChange inEffect = upcoming.takeEffect(MONDAY);

            assertThat(inEffect.getState()).isEqualTo(PlanChangeState.APPLIED);
            assertThat(inEffect.isInEffect()).isTrue();
            assertThat(inEffect.isUpcoming()).isFalse();
            assertThat(inEffect.getTookEffectAt()).contains(MONDAY);
            assertThat(inEffect.getEndedAt()).contains(ACCEPTED_AT);
            assertThat(inEffect.isUnsettled()).isFalse();
        }

        @Test
        void shouldRefuse_whenItIsNotUpcoming() {
            assertThatThrownBy(() -> pending().takeEffect(MONDAY)).isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> upcoming().takeEffect(MONDAY).takeEffect(MONDAY))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Expire {

        @Test
        void shouldEndNowWithTheCause_whenAnOpenOneExpires() {
            PlanChange accepted = pending().accept(Side.CLIENT, ACCEPTED_AT, TODAY);

            PlanChange expired = accepted.expire(ExpiryCause.UNDECIDED, MONDAY);

            assertThat(expired.getState()).isEqualTo(PlanChangeState.EXPIRED);
            assertThat(expired.getExpiryCause()).contains(ExpiryCause.UNDECIDED);
            assertThat(expired.getEndedAt()).contains(MONDAY);
            assertThat(expired.acceptedBy(Side.CLIENT)).isTrue();
            assertThat(expired.isUnsettled()).isFalse();
        }

        @Test
        void shouldKeepTheEarlierEndAndBothAcceptances_whenAnUpcomingOneExpires() {
            PlanChange expired = upcoming().expire(ExpiryCause.PLAN_EDITED, MONDAY);

            assertThat(expired.getExpiryCause()).contains(ExpiryCause.PLAN_EDITED);
            assertThat(expired.getEndedAt()).contains(ACCEPTED_AT);
            assertThat(expired.acceptedBy(Side.TRAINER)).isTrue();
            assertThat(expired.acceptedBy(Side.CLIENT)).isTrue();
        }

        @Test
        void shouldRefuse_whenItIsDeclinedInEffectOrAlreadyExpired() {
            PlanChange declined = pending().decline(Side.CLIENT, DeclineReason.OTHER, null, ACCEPTED_AT, TODAY);

            assertThatThrownBy(() -> declined.expire(ExpiryCause.UNDECIDED, MONDAY))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> upcoming().takeEffect(MONDAY).expire(ExpiryCause.PLAN_EDITED, MONDAY))
                    .isInstanceOf(IllegalStateException.class);
            assertThatThrownBy(() -> pending()
                            .expire(ExpiryCause.UNDECIDED, MONDAY)
                            .expire(ExpiryCause.UNDECIDED, MONDAY))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    class Decisions {

        @Test
        void shouldRefuseAcceptingAndUndoing_whenItExpired() {
            PlanChange expired = pending().expire(ExpiryCause.UNDECIDED, MONDAY);

            assertThatThrownBy(() -> expired.accept(Side.CLIENT, MONDAY, TODAY))
                    .isInstanceOf(PlanChangeEndedException.class);
            assertThatThrownBy(() -> expired.undo(Side.CLIENT, TODAY)).isInstanceOf(PlanChangeEndedException.class);
        }

        @Test
        void shouldRefuseUndoing_whenBothAcceptedAndItIsUpcoming() {
            assertThatThrownBy(() -> upcoming().undo(Side.CLIENT, TODAY)).isInstanceOf(PlanChangeEndedException.class);
        }
    }

    @Test
    void shouldRefuseToBeBuilt_whenItIsExpiredWithoutACause() {
        PlanChange pending = pending();

        assertThatThrownBy(() -> new PlanChange(
                        pending.getId(),
                        pending.getClient(),
                        Rule.HIGH_RPE,
                        PlanChangeState.EXPIRED,
                        3,
                        pending.getWindowStart(),
                        pending.getWindowEnd(),
                        EFFECTIVE,
                        pending.getContent(),
                        List.of(),
                        Set.of(SQUAT),
                        ACCEPTED_AT,
                        MONDAY,
                        null,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
