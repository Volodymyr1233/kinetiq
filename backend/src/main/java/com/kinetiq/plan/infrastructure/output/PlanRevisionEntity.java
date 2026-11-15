package com.kinetiq.plan.infrastructure.output;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

// written once with its sessions and never updated, so it has no version
@Getter
@Setter
@Entity
@Table(name = "plan_revisions")
class PlanRevisionEntity {

    @Id
    private UUID id;

    private UUID planId;
    private LocalDate validUntil;

    @CreationTimestamp
    private Instant createdAt;

    @OneToMany(mappedBy = "revision", cascade = CascadeType.ALL)
    private List<PlanRevisionSessionEntity> sessions = new ArrayList<>();
}
