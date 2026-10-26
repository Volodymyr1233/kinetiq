package com.kinetiq.account.infrastructure.output;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaAccountRepository extends JpaRepository<AccountEntity, UUID> {

    Optional<AccountEntity> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<AccountEntity> findByConfirmationTokenHash(String confirmationTokenHash);

    Optional<AccountEntity> findByResetTokenHash(String resetTokenHash);
}
