package com.kinetiq.readiness.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "check_ins")
class CheckInEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID clientId;
    private LocalDate checkInDate;
    private short sleep;
    private short soreness;
    private short stress;
    private Instant savedAt;
}
