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
import org.hibernate.annotations.BatchSize;

@Getter
@Setter
@Entity
@Table(name = "plan_revision_sessions")
class PlanRevisionSessionEntity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revision_id")
    private PlanRevisionEntity revision;

    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private DayOfWeek day;

    @ElementCollection
    @BatchSize(size = 50)
    @CollectionTable(name = "plan_revision_session_entries", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    private List<PlanEntryEmbeddable> entries = new ArrayList<>();
}
