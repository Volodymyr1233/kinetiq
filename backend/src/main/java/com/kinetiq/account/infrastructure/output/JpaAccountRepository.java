package com.kinetiq.account.infrastructure.output;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AccountEntity> findByConfirmationTokenHash(String confirmationTokenHash);

    Optional<AccountEntity> findByResetTokenHash(String resetTokenHash);

    List<AccountEntity> findByTrainerIdOrderByNameAsc(UUID trainerId);

    // sign-up always sends a link; an unconfirmed account without one (only demo data) is kept
    @Modifying
    @Query(
            nativeQuery = true,
            value = """
                    delete from accounts
                    where role = 'TRAINER' and not email_confirmed and confirmation_expires_at < :before
                    """)
    int deleteUnconfirmedTrainers(Instant before);

    int countByTrainerId(UUID trainerId);

    // a statement, not a loaded entity, so the database's cascades erase the rest (V27)
    @Modifying
    @Query(nativeQuery = true, value = "delete from accounts where id = :id")
    void deleteAccount(UUID id);
}
