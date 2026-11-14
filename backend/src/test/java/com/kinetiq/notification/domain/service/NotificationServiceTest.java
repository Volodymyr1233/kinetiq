package com.kinetiq.notification.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.notification.domain.exception.NotificationNotFoundException;
import com.kinetiq.notification.domain.model.Notification;
import com.kinetiq.notification.domain.model.NotificationId;
import com.kinetiq.notification.domain.model.NotificationsView;
import com.kinetiq.notification.domain.output.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class NotificationServiceTest {

    private static final AccountId TRAINER = new AccountId(UUID.randomUUID());
    private static final AccountId OTHER_TRAINER = new AccountId(UUID.randomUUID());
    private static final AccountId CLIENT = new AccountId(UUID.randomUUID());
    private static final Instant NOW = Instant.parse("2026-11-14T10:00:00Z");

    private final List<Notification> stored = new ArrayList<>();
    private final NotificationRepository repository = new NotificationRepository() {
        @Override
        public Notification save(Notification notification) {
            stored.removeIf(other -> other.id().equals(notification.id()));
            stored.add(notification);
            return notification;
        }

        @Override
        public List<Notification> latest(AccountId person, int limit) {
            return stored.stream()
                    .filter(notification -> notification.recipient().equals(person))
                    .sorted(Comparator.comparing(Notification::createdAt).reversed())
                    .limit(limit)
                    .toList();
        }

        @Override
        public long countUnread(AccountId person) {
            return stored.stream()
                    .filter(notification -> notification.recipient().equals(person) && !notification.isRead())
                    .count();
        }

        @Override
        public Optional<Notification> findOf(AccountId person, NotificationId id) {
            return stored.stream()
                    .filter(notification -> notification.id().equals(id) && notification.recipient().equals(person))
                    .findFirst();
        }
    };
    private final List<AccountId> told = new ArrayList<>();
    private final NotificationService service = new NotificationService(
            repository,
            ids -> ids.contains(CLIENT) ? Map.of(CLIENT, "Ola Kowalska") : Map.of(),
            told::add,
            Clock.fixed(NOW, ZoneOffset.UTC));

    private Notification add(AccountId recipient, NoticeKind kind, AccountId client, int minutesAgo) {
        Notification notification = new Notification(
                new NotificationId(UUID.randomUUID()), recipient, kind, client, NOW.minusSeconds(minutesAgo * 60L), null);
        stored.add(notification);
        return notification;
    }

    @Test
    void shouldStoreAnUnreadNotification_forTheRecipientOfANotice_andTellTheirStreams() {
        service.record(new Notice(TRAINER.value(), CLIENT.value(), NoticeKind.RECOVERY_DAY));

        Notification saved = stored.getFirst();
        assertThat(saved.recipient()).isEqualTo(TRAINER);
        assertThat(saved.client()).isEqualTo(CLIENT);
        assertThat(saved.kind()).isEqualTo(NoticeKind.RECOVERY_DAY);
        assertThat(saved.createdAt()).isEqualTo(NOW);
        assertThat(saved.isRead()).isFalse();
        assertThat(told).containsExactly(TRAINER);
    }

    @Test
    void shouldKeepNoClient_whenTheNoticeIsAboutNone() {
        service.record(new Notice(TRAINER.value(), null, NoticeKind.CLIENT_DELETED));

        assertThat(stored.getFirst().client()).isNull();
    }

    @Test
    void shouldListOnlyThePersonsOwn_newestFirst_withTheClientsCurrentName() {
        Notification older = add(TRAINER, NoticeKind.DRAFT_TO_REVIEW, CLIENT, 30);
        Notification newer = add(TRAINER, NoticeKind.RECOVERY_DAY, CLIENT, 5);
        add(OTHER_TRAINER, NoticeKind.DRAFT_TO_REVIEW, CLIENT, 1);

        NotificationsView view = service.list(TRAINER);

        assertThat(view.items()).extracting(item -> item.notification().id()).containsExactly(newer.id(), older.id());
        assertThat(view.items()).extracting("clientName").containsOnly("Ola Kowalska");
        assertThat(view.unread()).isEqualTo(2);
    }

    @Test
    void shouldShowTheLatestThirty_andCountEveryUnreadOne() {
        IntStream.range(0, 45).forEach(minutes -> add(TRAINER, NoticeKind.NEW_MESSAGE, CLIENT, minutes));

        NotificationsView view = service.list(TRAINER);

        assertThat(view.items()).hasSize(30);
        assertThat(view.unread()).isEqualTo(45);
    }

    @Test
    void shouldGiveNoNameForANotificationAboutNoClient() {
        add(TRAINER, NoticeKind.CLIENT_DELETED, null, 1);

        assertThat(service.list(TRAINER).items().getFirst().clientName()).isNull();
    }

    @Test
    void shouldMarkOneRead_andLeaveItAsItWasWhenReadAgain() {
        Notification notification = add(TRAINER, NoticeKind.PLAN_CHANGE_WAITING, CLIENT, 2);

        service.markRead(TRAINER, notification.id());
        Instant firstRead = stored.getFirst().readAt();
        service.markRead(TRAINER, notification.id());

        assertThat(firstRead).isEqualTo(NOW);
        assertThat(stored.getFirst().readAt()).isEqualTo(firstRead);
        assertThat(service.list(TRAINER).unread()).isZero();
    }

    @Test
    void shouldAnswerNotFound_forAnotherPersonsNotification_andChangeNothing() {
        Notification notification = add(TRAINER, NoticeKind.DRAFT_TO_REVIEW, CLIENT, 2);

        assertThatThrownBy(() -> service.markRead(OTHER_TRAINER, notification.id()))
                .isExactlyInstanceOf(NotificationNotFoundException.class);
        assertThatThrownBy(() -> service.markRead(TRAINER, new NotificationId(UUID.randomUUID())))
                .isExactlyInstanceOf(NotificationNotFoundException.class);
        assertThat(service.list(TRAINER).unread()).isEqualTo(1);
    }
}
