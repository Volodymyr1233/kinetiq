package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.DayOfWeek;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SessionDaysTest {

    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5, 6})
    void shouldGiveDistinctDaysMondayFirstInWeekOrder_whenAnyAllowedCount(int sessions) {
        List<DayOfWeek> days = SessionDays.forSessions(sessions);

        assertThat(days).hasSize(sessions).doesNotHaveDuplicates().isSorted();
        assertThat(days.getFirst()).isEqualTo(DayOfWeek.MONDAY);
        assertThat(days).doesNotContain(DayOfWeek.SUNDAY);
    }
}
