package com.kinetiq.notification.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.notification.domain.model.Notification;
import com.kinetiq.notification.domain.model.NotificationId;
import com.kinetiq.notification.domain.output.NotificationRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class NotificationRepositoryImpl implements NotificationRepository {

    private final JpaNotificationRepository jpa;

    @Override
    public Notification save(Notification notification) {
        NotificationEntity entity = jpa.findById(notification.id().value()).orElseGet(NotificationEntity::new);
        entity.setId(notification.id().value());
        entity.setRecipientId(notification.recipient().value());
        entity.setKind(notification.kind());
        entity.setClientId(notification.client() == null ? null : notification.client().value());
        entity.setReadAt(notification.readAt());
        return toDomain(jpa.saveAndFlush(entity));
    }

    @Override
    public List<Notification> latest(AccountId person, int limit) {
        return jpa.findByRecipientIdOrderByCreatedAtDesc(person.value(), PageRequest.ofSize(limit)).stream()
                .map(NotificationRepositoryImpl::toDomain)
                .toList();
    }

    @Override
    public long countUnread(AccountId person) {
        return jpa.countByRecipientIdAndReadAtIsNull(person.value());
    }

    @Override
    public Optional<Notification> findOf(AccountId person, NotificationId id) {
        return jpa.findByIdAndRecipientId(id.value(), person.value()).map(NotificationRepositoryImpl::toDomain);
    }

    private static Notification toDomain(NotificationEntity entity) {
        return new Notification(
                new NotificationId(entity.getId()),
                new AccountId(entity.getRecipientId()),
                entity.getKind(),
                entity.getClientId() == null ? null : new AccountId(entity.getClientId()),
                entity.getCreatedAt(),
                entity.getReadAt());
    }
}
