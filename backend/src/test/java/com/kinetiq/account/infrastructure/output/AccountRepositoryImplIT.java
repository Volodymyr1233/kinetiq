package com.kinetiq.account.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.model.WeightUnit;
import com.kinetiq.common.domain.model.Email;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;
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

    @Autowired
    private EntityManager entityManager;

    private static Account trainer(String email) {
        return Account.signUpTrainer("Marek Nowak", new Email(email), "hash", ZoneId.of("Europe/Warsaw"), Language.EN);
    }

    @Test
    void shouldFindChosenSettings_whenSavedAgain() {
        Account account = trainer("marek@example.com");
        repository.save(account);
        AccountSettings chosen = new AccountSettings(WeightUnit.LB, Language.PL, Theme.DARK);
        account.changeSettings(chosen);
        repository.save(account);
        jpa.flush();
        entityManager.clear();

        assertThat(repository.findById(account.getId()).orElseThrow().getSettings()).isEqualTo(chosen);
    }

    @Test
    void shouldStartWithKilogramsEnglishAndDeviceTheme_whenRowHasNoSettings() {
        UUID id = UUID.randomUUID();
        entityManager
                .createNativeQuery(
                        """
                        insert into accounts (id, name, email, password_hash, role, time_zone, email_confirmed,
                                              created_at, updated_at, version)
                        values (?1, 'Anna Kowalska', 'anna@example.com', 'hash', 'CLIENT', 'Europe/Warsaw', true,
                                now(), now(), 0)
                        """)
                .setParameter(1, id)
                .executeUpdate();

        assertThat(repository.findById(new AccountId(id)).orElseThrow().getSettings())
                .isEqualTo(new AccountSettings(WeightUnit.KG, Language.EN, Theme.SYSTEM));
    }

    @Test
    void shouldFail_whenUnitsAreUnknown() {
        Account account = trainer("marek@example.com");
        repository.save(account);
        jpa.flush();

        assertThatThrownBy(() -> entityManager
                        .createNativeQuery("update accounts set units = 'XX' where id = ?1")
                        .setParameter(1, account.getId().value())
                        .executeUpdate())
                .hasStackTraceContaining("ck_accounts_units");
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
    void shouldFindAccountWithChangedTimeZone_whenSavedAgain() {
        Account account = trainer("marek@example.com");
        repository.save(account);
        account.changeTimeZone(ZoneId.of("Europe/London"));
        repository.save(account);

        assertThat(repository.findById(account.getId()))
                .get()
                .extracting(Account::getTimeZone)
                .isEqualTo(ZoneId.of("Europe/London"));
    }

    @Test
    void shouldFindNothing_whenIdIsUnknown() {
        assertThat(repository.findById(AccountId.random())).isEmpty();
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
        SecretToken token = SecretToken.generate();
        account.issueConfirmation(token, Instant.parse("2026-10-19T10:00:00Z"));
        repository.save(account);

        assertThat(repository.findByConfirmationTokenHash(token.hash()))
                .get()
                .usingRecursiveComparison()
                .isEqualTo(account);
        assertThat(repository.findByConfirmationTokenHash(SecretToken.generate().hash()))
                .isEmpty();
    }

    @Test
    void shouldClearStoredLink_whenEmailIsConfirmed() {
        Account account = trainer("marek@example.com");
        SecretToken token = SecretToken.generate();
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
