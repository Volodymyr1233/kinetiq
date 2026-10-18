package com.kinetiq.account.infrastructure.config;

import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.account.domain.service.SignUpService;
import com.kinetiq.common.domain.output.MailSender;
import java.net.URI;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class AccountConfig {

    @Bean
    SignUpUseCase signUpUseCase(
            AccountRepository accounts,
            PasswordHasher hasher,
            MailSender mailSender,
            @Value("${kinetiq.app-url}") URI appUrl) {
        return new SignUpService(accounts, hasher, mailSender, Clock.systemUTC(), appUrl);
    }
}
