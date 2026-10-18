package com.kinetiq.account.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.ConfirmationToken;
import com.kinetiq.common.domain.model.Email;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({TestcontainersConfiguration.class, AccountRepositoryImpl.class, AccountMapperImpl.class})
class AccountRepositoryImplIT {

    @Autowired
    private AccountRepositoryImpl repository;

    @Autowired
    private JpaAccountRepository jpa;

    private static Account trainer(String email) {
        return Account.signUpTrainer("Marek Nowak", new Email(email), "hash", ZoneId.of("Europe/Warsaw"));
    }

    @Test
    void shouldFindAccount_whenEmailDiffersInLetterCase() {
        Account account = trainer("marek@example.com");
        repository.save(account);

        assertThat(repository.findByEmail(new Email("Marek@Example.COM")))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(account);
    }

    @Test
    void shouldFail_whenEmailIsTaken() {
        repository.save(trainer("marek@example.com"));
        jpa.flush();
        repository.save(trainer("marek@example.com"));

        assertThatThrownBy(jpa::flush).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldFindAccountWithItsLink_whenTokenHashMatches() {
        Account account = trainer("marek@example.com");
        ConfirmationToken token = ConfirmationToken.generate();
        account.issueConfirmation(token, Instant.parse("2026-10-19T10:00:00Z"));
        repository.save(account);

        assertThat(repository.findByConfirmationTokenHash(token.hash()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(account);
        assertThat(repository.findByConfirmationTokenHash(ConfirmationToken.generate().hash()))
                .isEmpty();
    }

    @Test
    void shouldClearStoredLink_whenEmailIsConfirmed() {
        Account account = trainer("marek@example.com");
        ConfirmationToken token = ConfirmationToken.generate();
        account.issueConfirmation(token, Instant.parse("2026-10-19T10:00:00Z"));
        repository.save(account);
        jpa.flush();

        account.confirmEmail(Instant.parse("2026-10-19T11:00:00Z"));
        repository.save(account);
        jpa.flush();

        assertThat(repository.findByConfirmationTokenHash(token.hash())).isEmpty();
        assertThat(repository.findByEmail(account.getEmail()).orElseThrow().isEmailConfirmed())
                .isTrue();
    }

    @Test
    void shouldSetTimestampsAndVersion_whenSaved() {
        Account account = trainer("marek@example.com");
        repository.save(account);
        jpa.flush();

        AccountEntity entity = jpa.findById(account.getId().value()).orElseThrow();
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getVersion()).isZero();
    }
}
