package com.kinetiq.account.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.infrastructure.input.RequestLimiter.Kind;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RequestLimiterTest {

    private static final String ADDRESS = "203.0.113.7";
    private static final String TRAINER = "00000000-0000-4000-8000-000000000001";

    private final MovableClock clock = new MovableClock();
    private final RequestLimiter limiter = new RequestLimiter(clock);

    private void acquireTimes(int times) {
        for (int i = 0; i < times; i++) {
            assertThat(limiter.tryAcquire(Kind.SIGN_UP, ADDRESS)).isTrue();
            clock.now = clock.now.plus(Duration.ofMinutes(1));
        }
    }

    @Test
    void shouldRefuseEleventh_whenTenCameWithinAnHour() {
        acquireTimes(10);

        assertThat(limiter.tryAcquire(Kind.SIGN_UP, ADDRESS)).isFalse();
    }

    @Test
    void shouldAllowAgain_whenTheFirstIsAnHourOld() {
        Instant first = clock.now;
        acquireTimes(10);

        clock.now = first.plus(Duration.ofHours(1));

        assertThat(limiter.tryAcquire(Kind.SIGN_UP, ADDRESS)).isTrue();
        assertThat(limiter.tryAcquire(Kind.SIGN_UP, ADDRESS)).isFalse();
    }

    @Test
    void shouldCountKindsAndAddressesApart() {
        acquireTimes(10);

        assertThat(limiter.tryAcquire(Kind.CONFIRMATION_LINK, ADDRESS)).isTrue();
        assertThat(limiter.tryAcquire(Kind.SIGN_UP, "198.51.100.2")).isTrue();
    }

    @Test
    void shouldRefuseTwentyFirstInvite_whenTwentyCameWithinAnHour() {
        Instant first = clock.now;
        for (int i = 0; i < 20; i++) {
            assertThat(limiter.tryAcquire(Kind.INVITE, TRAINER)).isTrue();
            clock.now = clock.now.plus(Duration.ofMinutes(1));
        }

        assertThat(limiter.tryAcquire(Kind.INVITE, TRAINER)).isFalse();
        clock.now = first.plus(Duration.ofHours(1));
        assertThat(limiter.tryAcquire(Kind.INVITE, TRAINER)).isTrue();
    }

    private static class MovableClock extends Clock {

        Instant now = Instant.parse("2026-10-24T10:00:00Z");

        @Override
        public Instant instant() {
            return now;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }
    }
}
