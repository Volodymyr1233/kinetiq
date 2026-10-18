package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface AccountMapper {

    // the aggregate has no setters, so this direction goes through its restore factory
    default Account toDomain(AccountEntity entity) {
        return Account.restore(
                new AccountId(entity.getId()),
                entity.getName(),
                new Email(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getRole(),
                ZoneId.of(entity.getTimeZone()),
                entity.isEmailConfirmed());
    }

    void update(Account account, @MappingTarget AccountEntity entity);

    default UUID map(AccountId id) {
        return id.value();
    }

    default String map(Email email) {
        return email.value();
    }

    default String map(ZoneId zone) {
        return zone.getId();
    }
}
