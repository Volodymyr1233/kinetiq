package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.InviteStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface JpaInviteRepository extends JpaRepository<InviteEntity, UUID> {

    List<InviteEntity> findByTrainerIdAndStatusAndExpiresAtAfterOrderBySentAtDesc(
            UUID trainerId, InviteStatus status, Instant now);

    Optional<InviteEntity> findByTrainerIdAndEmailAndStatus(UUID trainerId, String email, InviteStatus status);

    @Query("select i from InviteEntity i where i.emailTokenHash = :hash or i.linkTokenHash = :hash")
    Optional<InviteEntity> findByTokenHash(String hash);
}
