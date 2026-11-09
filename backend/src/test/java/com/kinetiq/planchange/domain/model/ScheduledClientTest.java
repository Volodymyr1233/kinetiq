package com.kinetiq.planchange.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ScheduledClientTest {

    private static ScheduledClient client(String zone, Optional<LocalDate> lastChecked) {
        return new ScheduledClient(AccountId.random(), ZoneId.of(zone), LocalDate.parse("2026-10-05"), 8, lastChecked);
    }

    private static LocalDate checkWeek(String zone, String instant) {
        return client(zone, Optional.empty()).checkWeek(Instant.parse(instant));
    }

    @Test
    void shouldStayInTheOldWeek_whenItIsOneMinuteBeforeMonday0300() {
        // Warsaw is UTC+1 in November
        assertThat(checkWeek("Europe/Warsaw", "2026-11-09T01:59:00Z")).isEqualTo(LocalDate.parse("2026-11-02"));
    }

    @Test
    void shouldMoveToTheNewWeek_whenItIsMonday0300() {
        assertThat(checkWeek("Europe/Warsaw", "2026-11-09T02:00:00Z")).isEqualTo(LocalDate.parse("2026-11-09"));
    }

    @Test
    void shouldUseTheClientsOwnZone_whenZonesDiffer() {
        // at 02:00Z it is Monday 03:00 in Warsaw and still Sunday 18:00 in Los Angeles
        assertThat(checkWeek("America/Los_Angeles", "2026-11-09T02:00:00Z")).isEqualTo(LocalDate.parse("2026-11-02"));
        assertThat(checkWeek("America/Los_Angeles", "2026-11-09T11:00:00Z")).isEqualTo(LocalDate.parse("2026-11-09"));
    }

    @Test
    void shouldHandleAHalfHourOffset_whenTheZoneHasOne() {
        // India is UTC+5:30: Monday 03:00 there is Sunday 21:30Z
        assertThat(checkWeek("Asia/Kolkata", "2026-11-08T21:29:00Z")).isEqualTo(LocalDate.parse("2026-11-02"));
        assertThat(checkWeek("Asia/Kolkata", "2026-11-08T21:30:00Z")).isEqualTo(LocalDate.parse("2026-11-09"));
    }

    @Test
    void shouldKeepMondayAt0300Local_whenSpringClocksMoveForward() {
        // Warsaw moves to UTC+2 on Sunday 2027-03-28
        assertThat(checkWeek("Europe/Warsaw", "2027-03-29T00:59:00Z")).isEqualTo(LocalDate.parse("2027-03-22"));
        assertThat(checkWeek("Europe/Warsaw", "2027-03-29T01:00:00Z")).isEqualTo(LocalDate.parse("2027-03-29"));
    }

    @Test
    void shouldKeepMondayAt0300Local_whenAutumnClocksMoveBack() {
        // Warsaw returns to UTC+1 on Sunday 2026-10-25
        assertThat(checkWeek("Europe/Warsaw", "2026-10-26T01:59:00Z")).isEqualTo(LocalDate.parse("2026-10-19"));
        assertThat(checkWeek("Europe/Warsaw", "2026-10-26T02:00:00Z")).isEqualTo(LocalDate.parse("2026-10-26"));
    }

    @Test
    void shouldBeDue_whenTheClientWasNeverChecked() {
        assertThat(client("Europe/Warsaw", Optional.empty()).isDue(Instant.parse("2026-11-04T12:00:00Z")))
                .isTrue();
    }

    @Test
    void shouldNotBeDue_whenTheZoneMovedWestAfterTheWeekWasChecked() {
        // checked for the week of 2026-11-09 as a Warsaw client; at 02:00Z it is still Sunday evening in Los Angeles
        ScheduledClient moved = client("America/Los_Angeles", Optional.of(LocalDate.parse("2026-11-09")));

        assertThat(moved.checkWeek(Instant.parse("2026-11-09T02:00:00Z"))).isEqualTo(LocalDate.parse("2026-11-02"));
        assertThat(moved.isDue(Instant.parse("2026-11-09T02:00:00Z"))).isFalse();
        assertThat(moved.isDue(Instant.parse("2026-11-16T11:00:00Z"))).isTrue();
    }

    @Test
    void shouldNotBeDue_whenThisWeekWasChecked_andBeDueAgainNextMonday() {
        ScheduledClient checked = client("Europe/Warsaw", Optional.of(LocalDate.parse("2026-11-02")));

        assertThat(checked.isDue(Instant.parse("2026-11-08T23:00:00Z"))).isFalse();
        assertThat(checked.isDue(Instant.parse("2026-11-09T02:00:00Z"))).isTrue();
    }
}
