package com.kinetiq.plan.infrastructure.output;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

// replaced with its plan on each save, so the plan's version covers it
@Getter
@Setter
@Entity
@Table(name = "plan_sessions")
class PlanSessionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id")
    private PlanEntity plan;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek day;

    @ElementCollection
    @CollectionTable(name = "plan_session_entries", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    private List<PlanEntryEmbeddable> entries = new ArrayList<>();
}
