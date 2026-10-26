package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.InviteStatus;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@Entity
@Table(name = "invites")
class InviteEntity extends BaseEntity {

    @Id
    private UUID id;

    private UUID trainerId;
    private String email;

    @Enumerated(EnumType.STRING)
    private InviteStatus status;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String emailTokenHash;

    @JdbcTypeCode(SqlTypes.CHAR)
    private String linkTokenHash;

    private Instant sentAt;
    private Instant expiresAt;
}
