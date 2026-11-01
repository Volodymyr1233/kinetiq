package com.kinetiq.plan.infrastructure.output;

import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
class PlanEntryEmbeddable {

    private UUID exerciseId;
    private int sets;
    private int amount;
    private BigDecimal loadKg;
    private int rpe;
    private int restSeconds;
}
