package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.InviteStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaInviteRepository extends JpaRepository<InviteEntity, UUID> {

    List<InviteEntity> findByTrainerIdAndStatusAndExpiresAtAfterOrderBySentAtDesc(
            UUID trainerId, InviteStatus status, Instant now);

    Optional<InviteEntity> findByTrainerIdAndEmailAndStatus(UUID trainerId, String email, InviteStatus status);

    @Query("select i from InviteEntity i where i.emailTokenHash = :hash or i.linkTokenHash = :hash")
    Optional<InviteEntity> findByTokenHash(String hash);

    // a used or revoked invite was last changed when it ended; an open one ends when it expires
    @Modifying
    @Query(
            nativeQuery = true,
            value = """
                    delete from invites
                    where (status <> 'OPEN' and updated_at < :before) or (status = 'OPEN' and expires_at < :before)
                    """)
    int deleteEnded(Instant before);

    @Modifying
    @Query(nativeQuery = true, value = "delete from invites where trainer_id = :trainerId and email = :email")
    void deleteByTrainerAndEmail(UUID trainerId, String email);
}
