package com.kinetiq.messaging.infrastructure.config;

import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.output.AboutSets;
import com.kinetiq.messaging.domain.output.ConversationDirectory;
import com.kinetiq.messaging.domain.output.ConversationParticipants;
import com.kinetiq.messaging.domain.output.ConversationRepository;
import com.kinetiq.messaging.domain.output.MessageRepository;
import com.kinetiq.messaging.domain.service.MessagingService;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class MessagingConfig {

    @Bean
    ExchangeMessagesUseCase exchangeMessagesUseCase(
            ConversationParticipants participants,
            ConversationRepository conversations,
            MessageRepository messages,
            ConversationDirectory directory,
            AboutSets aboutSets) {
        return new MessagingService(participants, conversations, messages, directory, aboutSets, Clock.systemUTC());
    }
}
