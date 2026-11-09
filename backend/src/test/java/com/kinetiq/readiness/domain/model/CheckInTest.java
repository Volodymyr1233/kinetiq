package com.kinetiq.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CheckInTest {

    private static final Instant NOW = Instant.parse("2026-11-08T08:00:00Z");

    private final CheckIn checkIn =
            CheckIn.give(AccountId.random(), LocalDate.parse("2026-11-08"), new Answers(2, 4, 3), NOW);

    @Test
    void shouldBringTheLighterTargetsBack_whenTheAnswersChange() {
        checkIn.choose(TargetChoice.ORIGINAL);

        checkIn.change(new Answers(1, 4, 3), NOW.plusSeconds(60));

        assertThat(checkIn.getTargetChoice()).isEqualTo(TargetChoice.LIGHTER);
    }

    @Test
    void shouldKeepTheChoice_whenTheSameAnswersAreSavedAgain() {
        checkIn.choose(TargetChoice.ORIGINAL);

        checkIn.change(new Answers(2, 4, 3), NOW.plusSeconds(60));

        assertThat(checkIn.getTargetChoice()).isEqualTo(TargetChoice.ORIGINAL);
    }
}
