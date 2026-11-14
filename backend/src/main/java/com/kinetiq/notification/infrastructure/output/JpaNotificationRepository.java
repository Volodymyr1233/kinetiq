package com.kinetiq.notification.infrastructure.output;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface JpaNotificationRepository extends JpaRepository<NotificationEntity, UUID> {

    List<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable limit);

    long countByRecipientIdAndReadAtIsNull(UUID recipientId);

    Optional<NotificationEntity> findByIdAndRecipientId(UUID id, UUID recipientId);
}
