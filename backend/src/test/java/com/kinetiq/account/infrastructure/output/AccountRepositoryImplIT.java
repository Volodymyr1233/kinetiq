package com.kinetiq.account.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.Role;
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
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
@Import({TestcontainersConfiguration.class, AccountRepositoryImpl.class, AccountMapperImpl.class})
class AccountRepositoryImplIT {

    @Autowired
    private AccountRepositoryImpl repository;

    @Autowired
    private JpaAccountRepository jpa;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbc;

    private static Account trainer(String email) {
        return Account.signUpTrainer("Marek Nowak", new Email(email), "hash", ZoneId.of("Europe/Warsaw"), Language.EN);
    }

    private static Account client(String name, String email, Account trainer) {
        return Account.restore(
                AccountId.random(),
                name,
                new Email(email),
                "hash",
                Role.CLIENT,
                trainer.getId(),
                ZoneId.of("Europe/Warsaw"),
                AccountSettings.startingWith(Language.EN),
                true,
                null,
                null,
                null);
    }

    @Test
    void shouldFindExactlyEachTrainersClientsOrderedByName_whenTwoTrainersHaveClients() {
        Account marek = trainer("marek@example.com");
        Account ola = trainer("ola@example.com");
        Account lonely = trainer("lonely@example.com");
        repository.save(marek);
        repository.save(ola);
        repository.save(lonely);
        Account zofia = client("Zofia Wójcik", "zofia@example.com", marek);
        Account anna = client("Anna Kowalska", "anna@example.com", marek);
        Account piotr = client("Piotr Lis", "piotr@example.com", ola);
        repository.save(zofia);
        repository.save(anna);
        repository.save(piotr);
        jpa.flush();
        entityManager.clear();

        assertThat(repository.findClientsOf(marek.getId()))
                .extracting(Account::getId)
                .containsExactly(anna.getId(), zofia.getId());
        assertThat(repository.findClientsOf(ola.getId()))
                .singleElement()
                .satisfies(found -> assertThat(found.getTrainerId()).contains(ola.getId()));
        assertThat(repository.findClientsOf(lonely.getId())).isEmpty();
    }

    @Test
    void shouldKeepNoTrainer_whenTrainerIsSavedAndFoundAgain() {
        Account account = trainer("marek@example.com");
        repository.save(account);
        jpa.flush();
        entityManager.clear();

        assertThat(repository.findById(account.getId()).orElseThrow().getTrainerId()).isEmpty();
    }

    @Test
    void shouldFail_whenClientRowHasNoTrainer() {
        assertThatThrownBy(() -> jdbc.update(
                        """
                        insert into accounts (id, name, email, password_hash, role, time_zone, email_confirmed,
                                              created_at, updated_at, version)
                        values (?, 'Anna Kowalska', 'anna@example.com', 'hash', 'CLIENT', 'Europe/Warsaw', true,
                                now(), now(), 0)
                        """,
                        UUID.randomUUID()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasStackTraceContaining("ck_accounts_client_trainer");
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
        Account trainer = trainer("marek@example.com");
        repository.save(trainer);
        jpa.flush();
        UUID id = UUID.randomUUID();
        entityManager
                .createNativeQuery(
                        """
                        insert into accounts (id, name, email, password_hash, role, trainer_id, time_zone,
                                              email_confirmed, created_at, updated_at, version)
                        values (?1, 'Anna Kowalska', 'anna@example.com', 'hash', 'CLIENT', ?2, 'Europe/Warsaw',
                                true, now(), now(), 0)
                        """)
                .setParameter(1, id)
                .setParameter(2, trainer.getId().value())
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
    void shouldFindAccountByResetToken_andNotAfterReset() {
        Account account = trainer("marek@example.com");
        SecretToken token = SecretToken.generate();
        account.issuePasswordReset(token, Instant.parse("2026-10-25T10:00:00Z"));
        repository.save(account);

        assertThat(repository.findByPasswordResetTokenHash(token.hash()))
                .get()
                .extracting(Account::getId)
                .isEqualTo(account.getId());

        account.resetPassword("new-hash", Instant.parse("2026-10-25T10:30:00Z"));
        repository.save(account);

        assertThat(repository.findByPasswordResetTokenHash(token.hash())).isEmpty();
        assertThat(repository.findById(account.getId()))
                .get()
                .extracting(Account::getPasswordHash)
                .isEqualTo("new-hash");
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
    void shouldTellWhetherEmailHasAccount_whenAsked() {
        repository.save(trainer("marek@example.com"));

        assertThat(repository.existsByEmail(new Email(" Marek@Example.com"))).isTrue();
        assertThat(repository.existsByEmail(new Email("anna@example.com"))).isFalse();
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
