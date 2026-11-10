package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.model.FigureKind;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Embeddable
class PlanChangeFigureEmbeddable {

    // a key of its own, because the exercise may be null and so cannot be part of one
    private UUID id;

    @Enumerated(EnumType.STRING)
    private FigureKind kind;

    private UUID exerciseId;
    private LocalDate weekStart;
    private BigDecimal value;
}
