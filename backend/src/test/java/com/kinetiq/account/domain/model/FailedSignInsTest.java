package com.kinetiq.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class FailedSignInsTest {

    private static final Instant START = Instant.parse("2026-10-24T10:00:00Z");

    private final FailedSignIns failed = new FailedSignIns(new Email("marek@example.com"));

    private static Instant at(int minutes, int seconds) {
        return START.plus(Duration.ofMinutes(minutes).plusSeconds(seconds));
    }

    private void failAt(Instant... times) {
        for (Instant time : times) {
            failed.recordFailure(time);
        }
    }

    @Test
    void shouldNotLock_whenFourAttemptsFailed() {
        failAt(at(0, 0), at(1, 0), at(2, 0), at(3, 0));

        assertThat(failed.isLocked(at(3, 1))).isFalse();
    }

    @Test
    void shouldLockFor15MinutesFromFifthFailure_whenFiveFailWithin15Minutes() {
        failAt(at(0, 0), at(1, 0), at(2, 0), at(3, 0), at(14, 0));

        assertThat(failed.isLocked(at(14, 0))).isTrue();
        assertThat(failed.isLocked(at(28, 59))).isTrue();
        assertThat(failed.timeLeft(at(28, 59))).contains(Duration.ofSeconds(1));
        assertThat(failed.isLocked(at(29, 0))).isFalse();
        assertThat(failed.timeLeft(at(29, 0))).isEmpty();
    }

    @Test
    void shouldNotCountFailure_whenItIs15MinutesOld() {
        failAt(at(0, 0), at(1, 0), at(2, 0), at(3, 0), at(15, 0));

        assertThat(failed.isLocked(at(15, 0))).isFalse();
    }

    @Test
    void shouldNeitherCountNorExtend_whenAttemptsComeWhileLocked() {
        failAt(at(0, 0), at(0, 1), at(0, 2), at(0, 3), at(0, 4));

        failAt(at(10, 0), at(14, 0));

        assertThat(failed.timeLeft(at(14, 0))).contains(Duration.ofSeconds(60 + 4));
    }

    @Test
    void shouldCountFromZero_whenLockHasEnded() {
        failAt(at(0, 0), at(0, 1), at(0, 2), at(0, 3), at(0, 4));

        failAt(at(15, 4), at(15, 5), at(15, 6), at(15, 7));

        assertThat(failed.isLocked(at(15, 8))).isFalse();
        failed.recordFailure(at(15, 8));
        assertThat(failed.isLocked(at(15, 8))).isTrue();
    }

    @Test
    void shouldBeExpired_onlyWhenNeitherLockedNorRecentlyFailed() {
        failAt(at(0, 0));

        assertThat(failed.isExpired(at(14, 59))).isFalse();
        assertThat(failed.isExpired(at(15, 0))).isTrue();
    }
}
