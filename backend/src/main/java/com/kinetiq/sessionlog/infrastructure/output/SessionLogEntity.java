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
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;

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

    // subselect fetching loads the collections of every log a query returned at once, not one log at a time
    @ElementCollection
    @CollectionTable(name = "session_log_entries", joinColumns = @JoinColumn(name = "session_log_id"))
    @OrderColumn(name = "position")
    @Fetch(FetchMode.SUBSELECT)
    private List<LoggedEntryEmbeddable> entries = new ArrayList<>();

    @OneToMany(mappedBy = "sessionLog", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<SetLogEntity> setLogs = new ArrayList<>();
}
