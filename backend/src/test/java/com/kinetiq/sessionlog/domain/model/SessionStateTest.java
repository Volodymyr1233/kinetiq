package com.kinetiq.sessionlog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SessionStateTest {

    // 2026-11-04 is a Wednesday
    private static final LocalDate WEDNESDAY = LocalDate.parse("2026-11-04");
    private static final LocalDate THURSDAY = WEDNESDAY.plusDays(1);
    private static final LocalDate MONDAY = LocalDate.parse("2026-11-02");
    private static final Instant STARTED = Instant.parse("2026-11-04T18:00:00Z");

    private static Optional<SessionLogHeader> log(Instant finishedAt) {
        return Optional.of(new SessionLogHeader(SessionLogId.random(), WEDNESDAY, STARTED, finishedAt));
    }

    private static Optional<SessionState> of(LocalDate date, LocalDate today, LocalDate published, Instant now) {
        return SessionState.of(date, today, published, Optional.empty(), now);
    }

    @Test
    void shouldBeTodayPlannedOrMissed_whenThereIsNoLog() {
        Instant now = Instant.parse("2026-11-04T09:00:00Z");

        assertThat(of(WEDNESDAY, WEDNESDAY, MONDAY, now)).contains(SessionState.TODAY);
        assertThat(of(THURSDAY, WEDNESDAY, MONDAY, now)).contains(SessionState.PLANNED);
        assertThat(of(WEDNESDAY, THURSDAY, MONDAY, now)).contains(SessionState.MISSED);
    }

    @Test
    void shouldBePlanned_forAFutureDayOfAnyWeek() {
        assertThat(of(WEDNESDAY.plusWeeks(9), WEDNESDAY, MONDAY, STARTED)).contains(SessionState.PLANNED);
    }

    @Test
    void shouldHaveNoState_whenTheDayPassedBeforeThePlanWasPublished() {
        assertThat(of(WEDNESDAY, THURSDAY.plusDays(1), THURSDAY, STARTED)).isEmpty();
    }

    @Test
    void shouldBeTodayOnThePublishDayAndMissedTheNextDay() {
        assertThat(of(THURSDAY, THURSDAY, THURSDAY, STARTED)).contains(SessionState.TODAY);
        assertThat(of(THURSDAY, THURSDAY.plusDays(1), THURSDAY, STARTED)).contains(SessionState.MISSED);
    }

    @Test
    void shouldBeInProgressUntilItLocks_thenMissed() {
        Instant justBefore = Instant.parse("2026-11-05T17:59:59Z");
        Instant lockMoment = Instant.parse("2026-11-05T18:00:00Z");

        assertThat(SessionState.of(WEDNESDAY, THURSDAY, MONDAY, log(null), justBefore))
                .contains(SessionState.IN_PROGRESS);
        assertThat(SessionState.of(WEDNESDAY, THURSDAY, MONDAY, log(null), lockMoment))
                .contains(SessionState.MISSED);
    }

    @Test
    void shouldBeDone_beforeAndAfterTheLock_andOnAPastDay() {
        Instant finished = STARTED.plusSeconds(1800);

        assertThat(SessionState.of(WEDNESDAY, WEDNESDAY, MONDAY, log(finished), finished))
                .contains(SessionState.DONE);
        assertThat(SessionState.of(WEDNESDAY, WEDNESDAY.plusWeeks(2), MONDAY, log(finished), STARTED.plusSeconds(86400 * 14)))
                .contains(SessionState.DONE);
    }

    @Test
    void shouldShowTheStateOfALog_evenOnADayBeforeThePublishDay() {
        Instant finished = STARTED.plusSeconds(1800);

        assertThat(SessionState.of(WEDNESDAY, THURSDAY, THURSDAY, log(finished), finished.plusSeconds(60)))
                .contains(SessionState.DONE);
    }
}
