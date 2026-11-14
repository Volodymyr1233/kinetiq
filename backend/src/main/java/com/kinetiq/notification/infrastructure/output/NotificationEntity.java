package com.kinetiq.notification.infrastructure.output;

import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notifications")
class NotificationEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID recipientId;
    private UUID clientId;
    private Instant readAt;

    @Enumerated(EnumType.STRING)
    private NoticeKind kind;
}
