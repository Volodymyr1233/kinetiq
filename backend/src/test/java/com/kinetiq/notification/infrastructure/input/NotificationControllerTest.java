package com.kinetiq.notification.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.notification.domain.exception.NotificationNotFoundException;
import com.kinetiq.notification.domain.input.ViewNotificationsUseCase;
import com.kinetiq.notification.domain.model.Notification;
import com.kinetiq.notification.domain.model.NotificationId;
import com.kinetiq.notification.domain.model.NotificationView;
import com.kinetiq.notification.domain.model.NotificationsView;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(NotificationController.class)
@Import({SecurityConfig.class, WebConfig.class})
class NotificationControllerTest {

    private static final AccountId TRAINER_ID =
            new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final String ADDRESS = "/api/v1/notifications";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewNotificationsUseCase notifications;

    @Test
    void shouldListTheSignedInPersonsNotifications_forATrainerAndForAClient() {
        NotificationId id = new NotificationId(UUID.randomUUID());
        Notification recovery = new Notification(
                id,
                TRAINER_ID,
                NoticeKind.RECOVERY_DAY,
                CLIENT_ID,
                Instant.parse("2026-11-14T10:00:00Z"),
                null);
        when(notifications.list(TRAINER_ID))
                .thenReturn(new NotificationsView(List.of(new NotificationView(recovery, "Ola Kowalska")), 1));
        when(notifications.list(CLIENT_ID)).thenReturn(new NotificationsView(List.of(), 0));

        var trainer = mvc.get().uri(ADDRESS).with(TRAINER).exchange();
        var client = mvc.get().uri(ADDRESS).with(CLIENT).exchange();

        assertThat(trainer).hasStatus(HttpStatus.OK);
        assertThat(trainer).bodyJson().extractingPath("$.unread").isEqualTo(1);
        assertThat(trainer).bodyJson().extractingPath("$.items[0].id").isEqualTo(id.value().toString());
        assertThat(trainer).bodyJson().extractingPath("$.items[0].kind").isEqualTo("RECOVERY_DAY");
        assertThat(trainer).bodyJson().extractingPath("$.items[0].read").isEqualTo(false);
        assertThat(trainer).bodyJson().extractingPath("$.items[0].clientName").isEqualTo("Ola Kowalska");
        assertThat(client).hasStatus(HttpStatus.OK);
        assertThat(client).bodyJson().extractingPath("$.items").asList().isEmpty();
    }

    @Test
    void shouldLeaveOutTheClient_whenTheNotificationIsAboutNoOne() {
        Notification deleted = new Notification(
                new NotificationId(UUID.randomUUID()),
                TRAINER_ID,
                NoticeKind.CLIENT_DELETED,
                null,
                Instant.parse("2026-11-14T10:00:00Z"),
                null);
        when(notifications.list(TRAINER_ID))
                .thenReturn(new NotificationsView(List.of(new NotificationView(deleted, null)), 1));

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).bodyJson().doesNotHavePath("$.items[0].clientId");
        assertThat(result).bodyJson().doesNotHavePath("$.items[0].clientName");
    }

    @Test
    void shouldMarkOneRead() {
        UUID id = UUID.randomUUID();

        var result = mvc.post().uri(ADDRESS + "/" + id + "/read").with(CLIENT).with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(notifications).markRead(CLIENT_ID, new NotificationId(id));
    }

    @Test
    void shouldAnswerNotFound_forAnotherPersonsNotification() {
        UUID id = UUID.randomUUID();
        doThrow(new NotificationNotFoundException()).when(notifications).markRead(TRAINER_ID, new NotificationId(id));

        var result = mvc.post().uri(ADDRESS + "/" + id + "/read").with(TRAINER).with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("notification.not-found");
    }

    @Test
    void shouldAskAVisitorToSignIn() {
        var result = mvc.get().uri(ADDRESS).exchange();

        assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("request.unauthenticated");
    }
}
