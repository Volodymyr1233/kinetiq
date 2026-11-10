package com.kinetiq.planchange.infrastructure.output;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.time.DayOfWeek;
import lombok.Getter;
import lombok.Setter;

// the session a move comes from is the removed one, so only its position is stored
@Getter
@Setter
@Embeddable
class PlanChangeMoveEmbeddable {

    private int fromPosition;

    @Enumerated(EnumType.STRING)
    private DayOfWeek toDay;
}
