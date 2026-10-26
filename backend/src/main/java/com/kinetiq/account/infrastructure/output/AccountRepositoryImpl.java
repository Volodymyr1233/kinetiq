package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.common.domain.model.Email;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class AccountRepositoryImpl implements AccountRepository {

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
}
