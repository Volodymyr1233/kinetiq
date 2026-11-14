package com.kinetiq.notification.infrastructure.config;

import com.kinetiq.notification.domain.output.ClientNames;
import com.kinetiq.notification.domain.output.NotificationEvents;
import com.kinetiq.notification.domain.output.NotificationRepository;
import com.kinetiq.notification.domain.service.NotificationService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class NotificationConfig {

    // one service behind both use cases: listing and marking read, and storing what the features raise
    @Bean
    NotificationService notificationService(
            NotificationRepository notifications, ClientNames names, NotificationEvents events) {
        return new NotificationService(notifications, names, events, Clock.systemUTC());
    }
}
