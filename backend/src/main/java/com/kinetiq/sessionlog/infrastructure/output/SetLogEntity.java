package com.kinetiq.sessionlog.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "set_logs")
class SetLogEntity extends BaseEntity {

    // made by the client
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_log_id")
    private SessionLogEntity sessionLog;

    private int entryPosition;
    private int setNumber;
    private BigDecimal loadKg;
    private int amount;
    private int rpe;
    private BigDecimal targetLoadKg;
    private int targetAmount;
    private int targetRpe;
    private Instant loggedAt;
}
