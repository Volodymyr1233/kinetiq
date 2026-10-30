package com.kinetiq.client.infrastructure.config;

import com.kinetiq.client.domain.input.ListClientsUseCase;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.client.domain.service.ClientListingService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClientConfig {

    @Bean
    ListClientsUseCase listClientsUseCase(ClientDirectory clients) {
        return new ClientListingService(clients);
    }
}
