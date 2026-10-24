package com.kinetiq.account.infrastructure.input;

import com.kinetiq.common.domain.exception.InvalidValueException;
import java.time.ZoneId;

final class TimeZones {

    private TimeZones() {}

    // region ids only: offsets such as +02:00 would not follow daylight saving time
    static ZoneId parse(String id) {
        if (!ZoneId.getAvailableZoneIds().contains(id)) {
            throw new InvalidValueException("unknown time zone");
        }
        return ZoneId.of(id);
    }
}
