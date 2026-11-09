package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlanChangeDetectionServiceTest {

    private final FakeTrainingHistory history = new FakeTrainingHistory();
    private final MutableClock clock = new MutableClock("2026-11-09T01:00:00Z");
    private final PlanChangeDetectionService service = new PlanChangeDetectionService(history, clock);

    private void checkAllDue() {
        for (AccountId client : service.due()) {
            service.check(client);
        }
    }

    private static List<LocalDate> weeks(String... mondays) {
        return Arrays.stream(mondays).map(LocalDate::parse).toList();
    }

    @Test
    void shouldCheckOnlyTheClientWhoseMondayHasBegun_whenZonesDiffer() {
        AccountId warsaw = history.publishedPlan("Europe/Warsaw");
        AccountId losAngeles = history.publishedPlan("America/Los_Angeles");
        history.checked(warsaw, "2026-11-02");
        history.checked(losAngeles, "2026-11-02");

        clock.set("2026-11-09T01:59:00Z");
        assertThat(service.due()).isEmpty();

        clock.set("2026-11-09T02:00:00Z");
        assertThat(service.due()).containsExactly(warsaw);

        clock.set("2026-11-09T11:00:00Z");
        assertThat(service.due()).containsExactlyInAnyOrder(warsaw, losAngeles);
    }

    @Test
    void shouldCheckOncePerWeek_whenCheckIsCalledAgain() {
        AccountId client = history.publishedPlan("Europe/Warsaw");
        clock.set("2026-11-09T02:00:00Z");

        service.check(client);
        clock.advance(Duration.ofDays(3));
        service.check(client);

        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2026-11-09"));
        assertThat(history.claims).hasSize(1);
    }

    @Test
    void shouldCheckLaterInTheWeek_whenTheRunWasMissedAtMonday0300() {
        AccountId client = history.publishedPlan("Europe/Warsaw");
        history.checked(client, "2026-11-02");

        clock.set("2026-11-13T17:00:00Z");
        checkAllDue();

        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2026-11-02", "2026-11-09"));
    }

    @Test
    void shouldCheckOnceInTheWeekOfAClockChange_whenMondayFollowsIt() {
        AccountId client = history.publishedPlan("Europe/Warsaw");
        history.checked(client, "2027-03-22");

        // Sunday 2027-03-28 loses an hour, so Monday 03:00 is 01:00Z
        clock.set("2027-03-29T00:59:00Z");
        checkAllDue();
        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2027-03-22"));

        clock.set("2027-03-29T01:00:00Z");
        checkAllDue();
        checkAllDue();
        assertThat(history.claimedWeeks(client)).isEqualTo(weeks("2027-03-22", "2027-03-29"));
    }

    @Test
    void shouldSkipTheClient_whenThereIsNoPublishedPlan() {
        service.check(AccountId.random());

        assertThat(history.claims).isEmpty();
    }

    @Test
    void shouldCheckEachClientExactlyOncePerWeek_overAMonth() {
        AccountId warsaw = history.publishedPlan("Europe/Warsaw");
        AccountId losAngeles = history.publishedPlan("America/Los_Angeles");
        history.checked(warsaw, "2026-10-26");
        history.checked(losAngeles, "2026-10-26");
        clock.set("2026-11-02T00:00:00Z");

        // a tick every 10 minutes for 28 days, as the sweep runs
        for (int tick = 0; tick < 28 * 24 * 6; tick++) {
            checkAllDue();
            clock.advance(Duration.ofMinutes(10));
        }

        List<LocalDate> mondays = weeks("2026-10-26", "2026-11-02", "2026-11-09", "2026-11-16", "2026-11-23");
        assertThat(history.claimedWeeks(warsaw)).isEqualTo(mondays);
        assertThat(history.claimedWeeks(losAngeles)).isEqualTo(mondays);
        assertThat(history.claims).hasSize(8);
    }
}
