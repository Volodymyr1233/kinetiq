package com.kinetiq.plan.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PlanConfigTest {

    @Test
    void shouldEndDraftingAfterFiveAndAHalfMinutes_whenAttemptsTakeTheDefault150Seconds() {
        // with the sweep's 30 second interval, that keeps the 6 minute promise of SC-002
        assertThat(PlanConfig.draftingLimit(Duration.ofSeconds(150))).isEqualTo(Duration.ofMinutes(5).plusSeconds(30));
    }
}
