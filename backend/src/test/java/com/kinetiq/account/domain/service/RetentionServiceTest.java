package com.kinetiq.account.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RetentionServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-12T10:00:00Z");

    private final InMemoryAccountRepository accounts = new InMemoryAccountRepository();
    private final InMemoryInviteRepository invites = new InMemoryInviteRepository();
    private final RetentionService service = new RetentionService(accounts, invites, new MutableClock(NOW));

    private Account unconfirmedTrainer(String email, Instant linkSentAt) {
        Account account = Account.signUpTrainer("Marek", new Email(email), "hash", ZoneId.of("UTC"), Language.EN);
        account.issueConfirmation(SecretToken.generate(), linkSentAt);
        return accounts.add(account);
    }

    @Test
    void shouldEraseAnUnconfirmedTrainer_whenTheLastLinkWasSentMoreThanSevenDaysAgo() {
        Account old = unconfirmedTrainer("old@example.com", NOW.minus(Duration.ofDays(7)).minusSeconds(60));
        Account recent = unconfirmedTrainer("recent@example.com", NOW.minus(Duration.ofDays(7)).plusSeconds(60));

        service.eraseExpired();

        assertThat(accounts.findById(old.getId())).isEmpty();
        assertThat(accounts.findById(recent.getId())).isPresent();
    }

    @Test
    void shouldEraseInvitesThatEndedMoreThanAnHourAgo() {
        service.eraseExpired();

        assertThat(invites.endedBefore).isEqualTo(NOW.minus(Duration.ofHours(1)));
    }
}
