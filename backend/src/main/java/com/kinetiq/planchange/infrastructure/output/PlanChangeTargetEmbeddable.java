package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.model.Target;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
class PlanChangeTargetEmbeddable {

    @Enumerated(EnumType.STRING)
    private DayOfWeek sessionDay;

    private int position;

    @Enumerated(EnumType.STRING)
    private Target target;

    private UUID exerciseId;
    private String sessionName;
    private BigDecimal oldValue;
    private BigDecimal newValue;
}
