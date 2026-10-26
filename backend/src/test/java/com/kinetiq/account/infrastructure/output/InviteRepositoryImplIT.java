package com.kinetiq.account.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.TestcontainersConfiguration;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.SentInvite;
import com.kinetiq.common.domain.model.Email;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@DataJpaTest
@Import({
    TestcontainersConfiguration.class,
    InviteRepositoryImpl.class,
    InviteMapperImpl.class,
    AccountRepositoryImpl.class,
    AccountMapperImpl.class
})
class InviteRepositoryImplIT {

    private static final Instant NOW = Instant.parse("2026-10-25T10:00:00Z");
    private static final Email JULIA = new Email("julia@example.com");

    @Autowired
    private InviteRepositoryImpl repository;

    @Autowired
    private AccountRepositoryImpl accounts;

    private AccountId trainer;
    private AccountId otherTrainer;

    @BeforeEach
    void storeTrainers() {
        trainer = storeTrainer("marek@example.com");
        otherTrainer = storeTrainer("anna@example.com");
    }

    private AccountId storeTrainer(String email) {
        Account account = Account.signUpTrainer("Trainer", new Email(email), "hash", ZoneId.of("Europe/Warsaw"), Language.EN);
        accounts.save(account);
        return account.getId();
    }

    private SentInvite store(AccountId trainerId, String email, Instant sentAt) {
        SentInvite sent = Invite.send(trainerId, new Email(email), sentAt);
        repository.save(sent.invite());
        return sent;
    }

    @Test
    void shouldRestoreEveryField_whenSaved() {
        Invite invite = store(trainer, "julia@example.com", NOW).invite();

        assertThat(repository.findById(invite.getId())).get().usingRecursiveComparison().isEqualTo(invite);
    }

    @Test
    void shouldFindTheSameInvite_whenEitherSecretMatches() {
        SentInvite sent = store(trainer, "julia@example.com", NOW);

        assertThat(repository.findByTokenHash(sent.emailToken().hash()))
                .get()
                .extracting(Invite::getId)
                .isEqualTo(sent.invite().getId());
        assertThat(repository.findByTokenHash(sent.linkToken().hash()))
                .get()
                .extracting(Invite::getId)
                .isEqualTo(sent.invite().getId());
        assertThat(repository.findByTokenHash(SecretToken.generate().hash())).isEmpty();
    }

    @Test
    void shouldNotFindTheOldLink_whenLinkWasReplaced() {
        SentInvite sent = store(trainer, "julia@example.com", NOW);
        Invite invite = sent.invite();
        invite.replaceLink(NOW);
        repository.save(invite);

        assertThat(repository.findByTokenHash(sent.linkToken().hash())).isEmpty();
        assertThat(repository.findByTokenHash(invite.getLinkTokenHash())).isPresent();
    }

    @Test
    void shouldListOnlyOwnOpenUnexpiredInvites_newestFirst() {
        Invite older = store(trainer, "adam@example.com", NOW.minus(Duration.ofDays(2))).invite();
        Invite newer = store(trainer, "julia@example.com", NOW).invite();
        store(trainer, "kasia@example.com", NOW.minus(Duration.ofDays(8)));
        Invite revoked = store(trainer, "piotr@example.com", NOW).invite();
        revoked.revoke(NOW);
        repository.save(revoked);
        Invite used = store(trainer, "ola@example.com", NOW).invite();
        used.use(NOW);
        repository.save(used);
        store(otherTrainer, "ewa@example.com", NOW);

        assertThat(repository.findOpenByTrainer(trainer, NOW))
                .extracting(Invite::getId)
                .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void shouldFindOpenInviteForEmail_whenItHasExpired() {
        Invite expired = store(trainer, "julia@example.com", NOW.minus(Duration.ofDays(8))).invite();

        assertThat(repository.findOpenByTrainerAndEmail(trainer, JULIA))
                .get()
                .extracting(Invite::getId)
                .isEqualTo(expired.getId());
        assertThat(repository.findOpenByTrainerAndEmail(otherTrainer, JULIA)).isEmpty();
    }

    @Test
    void shouldRefuseSecondOpenInvite_whenSameTrainerAndEmail() {
        store(trainer, "julia@example.com", NOW);
        Invite second = Invite.send(trainer, JULIA, NOW).invite();

        assertThatThrownBy(() -> repository.save(second)).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void shouldAllowNewOpenInvite_whenEarlierOneWasSuperseded() {
        Invite first = store(trainer, "julia@example.com", NOW).invite();
        first.supersede();
        repository.save(first);

        store(trainer, "julia@example.com", NOW);

        assertThat(repository.findOpenByTrainer(trainer, NOW)).hasSize(1);
    }
}
