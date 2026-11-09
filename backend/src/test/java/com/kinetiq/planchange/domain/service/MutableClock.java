package com.kinetiq.planchange.domain.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock the test moves forward. */
class MutableClock extends Clock {

    private Instant now;

    MutableClock(String start) {
        now = Instant.parse(start);
    }

    void advance(Duration duration) {
        now = now.plus(duration);
    }

    void set(String instant) {
        now = Instant.parse(instant);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Instant instant() {
        return now;
    }
}
