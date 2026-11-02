package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

// read only: rows are written by one upsert in JpaMessageRepository, so a repeated id does not abort the transaction
@Getter
@Entity
@Table(name = "messages")
class MessageEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID conversationId;
    private UUID senderId;
    private String text;
    private Instant receivedAt;
}
