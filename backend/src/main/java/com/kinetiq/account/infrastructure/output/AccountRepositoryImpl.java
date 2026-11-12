package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.exception.TrainerHasClientsException;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.common.domain.model.Email;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class AccountRepositoryImpl implements AccountRepository {

    private static final String CLIENT_TRAINER = "fk_accounts_trainer";

    private final JpaAccountRepository jpa;
    private final AccountMapper mapper;

    // updates the managed entity, so its version and timestamps survive and optimistic locking applies
    @Override
    public void save(Account account) {
        AccountEntity entity = jpa.findById(account.getId().value()).orElseGet(AccountEntity::new);
        mapper.update(account, entity);
        jpa.save(entity);
    }

    @Override
    public Optional<Account> findById(AccountId id) {
        return jpa.findById(id.value()).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByEmail(Email email) {
        return jpa.findByEmail(email.value()).map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpa.existsByEmail(email.value());
    }

    @Override
    public Optional<Account> findByConfirmationTokenHash(String tokenHash) {
        return jpa.findByConfirmationTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public Optional<Account> findByPasswordResetTokenHash(String tokenHash) {
        return jpa.findByResetTokenHash(tokenHash).map(mapper::toDomain);
    }

    @Override
    public List<Account> findClientsOf(AccountId trainerId) {
        return jpa.findByTrainerIdOrderByNameAsc(trainerId.value()).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public int deleteUnconfirmedTrainers(Instant linkExpiredBefore) {
        return jpa.deleteUnconfirmedTrainers(linkExpiredBefore);
    }

    @Override
    public int countClientsOf(AccountId trainerId) {
        return jpa.countByTrainerId(trainerId.value());
    }

    @Override
    public void delete(AccountId id) {
        try {
            jpa.deleteAccount(id.value());
        } catch (DataIntegrityViolationException exception) {
            // a client joined after the check; the failed statement ends the transaction, so the count is not read again
            if (exception.getCause() instanceof ConstraintViolationException violation
                    && CLIENT_TRAINER.equals(violation.getConstraintName())) {
                throw new TrainerHasClientsException(1);
            }
            throw exception;
        }
    }
}
