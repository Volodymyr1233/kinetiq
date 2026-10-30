package com.kinetiq.client.infrastructure.config;

import com.kinetiq.client.domain.input.ViewClientsUseCase;
import com.kinetiq.client.domain.output.ClientDirectory;
import com.kinetiq.client.domain.service.ClientViewService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class ClientConfig {

    @Bean
    ViewClientsUseCase viewClientsUseCase(ClientDirectory clients) {
        return new ClientViewService(clients);
    }
}
