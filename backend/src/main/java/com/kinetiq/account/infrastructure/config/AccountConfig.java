package com.kinetiq.account.infrastructure.config;

import com.kinetiq.account.domain.input.CurrentAccountUseCase;
import com.kinetiq.account.domain.input.ManageInvitesUseCase;
import com.kinetiq.account.domain.input.ResetPasswordUseCase;
import com.kinetiq.account.domain.input.SignInUseCase;
import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.AccountSessions;
import com.kinetiq.account.domain.output.FailedSignInsRepository;
import com.kinetiq.account.domain.output.InviteRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.account.domain.service.CurrentAccountService;
import com.kinetiq.account.domain.service.InviteManagementService;
import com.kinetiq.account.domain.service.PasswordResetService;
import com.kinetiq.account.domain.service.SignInService;
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

    @Bean
    ResetPasswordUseCase resetPasswordUseCase(
            AccountRepository accounts,
            PasswordHasher hasher,
            AccountSessions sessions,
            MailSender mailSender,
            @Value("${kinetiq.app-url}") URI appUrl) {
        return new PasswordResetService(accounts, hasher, sessions, mailSender, Clock.systemUTC(), appUrl);
    }

    @Bean
    ManageInvitesUseCase manageInvitesUseCase(
            InviteRepository invites,
            AccountRepository accounts,
            MailSender mailSender,
            @Value("${kinetiq.app-url}") URI appUrl) {
        return new InviteManagementService(invites, accounts, mailSender, Clock.systemUTC(), appUrl);
    }

    @Bean
    CurrentAccountUseCase currentAccountUseCase(AccountRepository accounts) {
        return new CurrentAccountService(accounts);
    }

    @Bean
    SignInUseCase signInUseCase(
            AccountRepository accounts, PasswordHasher hasher, FailedSignInsRepository failedSignIns) {
        return new SignInService(accounts, hasher, failedSignIns, Clock.systemUTC());
    }
}
