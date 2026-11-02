package com.kinetiq.messaging.infrastructure.output;

import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;

// read only: rows are written by one upsert in JpaConversationRepository, so two first sends never collide
@Getter
@Entity
@Table(name = "conversations")
class ConversationEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID clientId;
    private UUID trainerId;
}
