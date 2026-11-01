package com.kinetiq.plan.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import com.kinetiq.quiz.domain.model.Goal;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "plans")
class PlanEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID clientId;

    @Enumerated(EnumType.STRING)
    private Goal goal;

    private int weeks;
    private LocalDate firstWeek;

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlanSessionEntity> sessions = new ArrayList<>();
}
