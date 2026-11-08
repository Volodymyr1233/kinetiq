package com.kinetiq.sessionlog.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "session_logs")
class SessionLogEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID clientId;
    private LocalDate logDate;
    private String sessionName;
    private Instant startedAt;
    private Instant finishedAt;
    private String note;

    @ElementCollection
    @CollectionTable(name = "session_log_entries", joinColumns = @JoinColumn(name = "session_log_id"))
    @OrderColumn(name = "position")
    private List<LoggedEntryEmbeddable> entries = new ArrayList<>();

    @OneToMany(mappedBy = "sessionLog", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SetLogEntity> setLogs = new ArrayList<>();
}
