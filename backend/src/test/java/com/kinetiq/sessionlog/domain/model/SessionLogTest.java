package com.kinetiq.sessionlog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.exception.DomainException;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.sessionlog.domain.exception.SessionLogLockedException;
import com.kinetiq.sessionlog.domain.exception.SetLogConflictException;
import com.kinetiq.sessionlog.domain.exception.SetValuesInvalidException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SessionLogTest {

    private static final Instant NOW = Instant.parse("2026-11-07T10:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-07T10:05:00Z");
    private static final Instant LOCKS_AT = Instant.parse("2026-11-08T10:00:00Z");
    private static final LoggedEntry SQUAT =
            new LoggedEntry(ExerciseId.random(), 3, 5, new BigDecimal("100.0"), 8, 120);
    private static final LoggedEntry PLANK = new LoggedEntry(ExerciseId.random(), 2, 45, null, 7, 60);

    private static SessionLog started() {
        return SessionLog.start(
                AccountId.random(), LocalDate.parse("2026-11-07"), "Lower body", List.of(SQUAT, PLANK), NOW);
    }

    private static SetLogId newId() {
        return new SetLogId(UUID.randomUUID());
    }

    private static SetValues asPlanned() {
        return new SetValues(new BigDecimal("100.0"), 5, 8);
    }

    @Nested
    class Start {

        @Test
        void shouldKeepACopyOfThePlannedSession() {
            SessionLog log = started();

            assertThat(log.getName()).isEqualTo("Lower body");
            assertThat(log.getEntries()).containsExactly(SQUAT, PLANK);
            assertThat(log.getSetLogs()).isEmpty();
            assertThat(log.getFinishedAt()).isEmpty();
        }
    }

    @Nested
    class LogSet {

        @Test
        void shouldKeepTheTargetsOfTheSetItLogged() {
            SessionLog log = started();

            SetLog logged = log.logSet(newId(), 0, 1, new SetValues(new BigDecimal("105.0"), 4, 9), NOW);

            assertThat(logged.targetLoadKg()).isEqualByComparingTo("100.0");
            assertThat(logged.targetAmount()).isEqualTo(5);
            assertThat(logged.targetRpe()).isEqualTo(8);
            assertThat(logged.values().loadKg()).isEqualByComparingTo("105.0");
        }

        @Test
        void shouldStoreOneSet_whenTheSameIdIsSentTwiceWithTheSameValues() {
            SessionLog log = started();
            SetLogId id = newId();

            SetLog first = log.logSet(id, 0, 1, asPlanned(), NOW);
            SetLog again = log.logSet(id, 0, 1, asPlanned(), LATER);

            assertThat(log.getSetLogs()).hasSize(1);
            assertThat(again).isEqualTo(first);
            assertThat(again.loggedAt()).isEqualTo(NOW);
        }

        @Test
        void shouldReplaceTheValuesAndKeepTheTargets_whenTheSameIdIsSentWithNewValues() {
            SessionLog log = started();
            SetLogId id = newId();
            log.logSet(id, 0, 1, asPlanned(), NOW);

            SetLog changed = log.logSet(id, 0, 1, new SetValues(new BigDecimal("102.5"), 6, 9), LATER);

            assertThat(log.getSetLogs()).hasSize(1);
            assertThat(changed.values()).isEqualTo(new SetValues(new BigDecimal("102.5"), 6, 9));
            assertThat(changed.loggedAt()).isEqualTo(LATER);
            assertThat(changed.targetAmount()).isEqualTo(5);
        }

        @Test
        void shouldLogAnExerciseWithoutALoad() {
            SessionLog log = started();

            SetLog logged = log.logSet(newId(), 1, 2, new SetValues(null, 45, 7), NOW);

            assertThat(logged.values().loadKg()).isNull();
            assertThat(logged.targetLoadKg()).isNull();
        }

        @Test
        void shouldRefuseAPlaceTheSessionDoesNotHave() {
            SessionLog log = started();

            for (int[] place : new int[][] {{2, 1}, {-1, 1}, {0, 0}, {0, 4}, {1, 3}}) {
                assertThatThrownBy(() -> log.logSet(newId(), place[0], place[1], asPlanned(), NOW))
                        .isInstanceOf(SetValuesInvalidException.class)
                        .extracting(error -> ((DomainException) error).properties())
                        .isEqualTo(java.util.Map.of("field", "place"));
            }
            assertThat(log.getSetLogs()).isEmpty();
        }

        @Test
        void shouldRefuseAnIdThatAlreadyLogsAnotherPlace() {
            SessionLog log = started();
            SetLogId id = newId();
            log.logSet(id, 0, 1, asPlanned(), NOW);

            assertThatThrownBy(() -> log.logSet(id, 0, 2, asPlanned(), LATER))
                    .isInstanceOf(SetLogConflictException.class)
                    .extracting(error -> ((DomainException) error).code())
                    .isEqualTo("set-log.id-in-use");
        }

        @Test
        void shouldRefuseAPlaceThatIsLoggedUnderAnotherId() {
            SessionLog log = started();
            log.logSet(newId(), 0, 1, asPlanned(), NOW);

            assertThatThrownBy(() -> log.logSet(newId(), 0, 1, asPlanned(), LATER))
                    .isInstanceOf(SetLogConflictException.class)
                    .extracting(error -> ((DomainException) error).code())
                    .isEqualTo("set-log.place-taken");
            assertThat(log.getSetLogs()).hasSize(1);
        }
    }

    @Nested
    class Lock {

        @Test
        void shouldTakeANewSetAndAChange_whenTheSessionLocksANanosecondLater() {
            SessionLog log = started();
            SetLogId id = newId();
            log.logSet(id, 0, 1, asPlanned(), NOW);
            Instant last = LOCKS_AT.minusNanos(1);

            SetLog changed = log.logSet(id, 0, 1, new SetValues(new BigDecimal("102.5"), 5, 8), last);
            SetLog added = log.logSet(newId(), 0, 2, asPlanned(), last);

            assertThat(log.getSetLogs()).containsExactly(changed, added);
        }

        @Test
        void shouldRefuseANewSetAndAChange_andKeepTheSet_whenTheSessionHasLocked() {
            SessionLog log = started();
            SetLogId id = newId();
            SetLog logged = log.logSet(id, 0, 1, asPlanned(), NOW);

            assertThatThrownBy(() -> log.logSet(id, 0, 1, new SetValues(new BigDecimal("102.5"), 5, 8), LOCKS_AT))
                    .isInstanceOf(SessionLogLockedException.class)
                    .extracting(error -> ((DomainException) error).code())
                    .isEqualTo("session-log.locked");
            assertThatThrownBy(() -> log.logSet(newId(), 0, 2, asPlanned(), LOCKS_AT))
                    .isInstanceOf(SessionLogLockedException.class);

            assertThat(log.getSetLogs()).containsExactly(logged);
        }

        @Test
        void shouldAnswerARepeatAsTheFirstTime_whenTheSessionHasLocked() {
            SessionLog log = started();
            SetLogId id = newId();
            SetLog logged = log.logSet(id, 0, 1, asPlanned(), NOW);

            assertThat(log.logSet(id, 0, 1, asPlanned(), LOCKS_AT)).isEqualTo(logged);
            assertThat(log.logSet(id, 0, 1, asPlanned(), LOCKS_AT.plusSeconds(3600))).isEqualTo(logged);
            assertThat(log.getSetLogs()).containsExactly(logged);
        }

        @Test
        void shouldTakeAChange_whenTheSessionWasFinishedButHasNotLocked() {
            SessionLog finished = new SessionLog(
                    SessionLogId.random(),
                    AccountId.random(),
                    LocalDate.parse("2026-11-07"),
                    "Lower body",
                    NOW,
                    LATER,
                    List.of(SQUAT, PLANK),
                    List.of(),
                    0);

            SetLog logged = finished.logSet(newId(), 0, 1, asPlanned(), LOCKS_AT.minusNanos(1));

            assertThat(finished.getSetLogs()).containsExactly(logged);
        }
    }

    @Nested
    class Values {

        @Test
        void shouldAcceptTheLimits() {
            assertThat(new SetValues(null, 1, 5)).isNotNull();
            assertThat(new SetValues(new BigDecimal("999.9"), 3600, 10)).isNotNull();
            assertThat(new SetValues(new BigDecimal("0.1"), 8, 7)).isNotNull();
        }

        @Test
        void shouldRefuseAnAmountOutsideOneToThreeThousandSixHundred() {
            assertThatThrownBy(() -> new SetValues(null, 0, 8)).hasMessageContaining("amount");
            assertThatThrownBy(() -> new SetValues(null, 3601, 8)).hasMessageContaining("amount");
        }

        @Test
        void shouldRefuseAnRpeOutsideFiveToTen() {
            assertThatThrownBy(() -> new SetValues(null, 8, 4)).hasMessageContaining("rpe");
            assertThatThrownBy(() -> new SetValues(null, 8, 11)).hasMessageContaining("rpe");
        }

        @Test
        void shouldRefuseALoadThatIsNotAboveZero_orTooHeavy_orHasTwoDecimals() {
            assertThatThrownBy(() -> new SetValues(BigDecimal.ZERO, 8, 8)).hasMessageContaining("loadKg");
            assertThatThrownBy(() -> new SetValues(new BigDecimal("-5"), 8, 8)).hasMessageContaining("loadKg");
            assertThatThrownBy(() -> new SetValues(new BigDecimal("1000.0"), 8, 8)).hasMessageContaining("loadKg");
            assertThatThrownBy(() -> new SetValues(new BigDecimal("100.25"), 8, 8)).hasMessageContaining("loadKg");
        }
    }
}
