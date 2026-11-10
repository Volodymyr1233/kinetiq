package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.Rule;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "plan_changes")
class PlanChangeEntity {

    @Id
    private UUID id;

    private UUID clientId;

    @Enumerated(EnumType.STRING)
    private Rule rule;

    @Enumerated(EnumType.STRING)
    private PlanChangeState state;

    private long planVersion;
    private LocalDate windowStart;
    private LocalDate windowEnd;
    private LocalDate effectiveWeek;

    @Enumerated(EnumType.STRING)
    private DayOfWeek removedDay;

    private String removedName;
    private Instant createdAt;
    private Instant endedAt;

    @ElementCollection
    @CollectionTable(name = "plan_change_targets", joinColumns = @JoinColumn(name = "plan_change_id"))
    private List<PlanChangeTargetEmbeddable> targets = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "plan_change_moves", joinColumns = @JoinColumn(name = "plan_change_id"))
    private List<PlanChangeMoveEmbeddable> moves = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "plan_change_figures", joinColumns = @JoinColumn(name = "plan_change_id"))
    private List<PlanChangeFigureEmbeddable> figures = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "plan_change_exercises", joinColumns = @JoinColumn(name = "plan_change_id"))
    @Column(name = "exercise_id")
    private Set<UUID> exercises = new HashSet<>();
}
