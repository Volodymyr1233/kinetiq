package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.Role;
import com.kinetiq.common.infrastructure.output.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "accounts")
class AccountEntity extends BaseEntity {

    @Id
    private UUID id;

    private String name;
    private String email;
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    private Role role;

    private String timeZone;
    private boolean emailConfirmed;
}
