package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.EmailConfirmation;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import java.util.UUID;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface AccountMapper {

    // the aggregate has no setters, so this direction goes through its restore factory
    default Account toDomain(AccountEntity entity) {
        EmailConfirmation confirmation = entity.getConfirmationTokenHash() == null
                ? null
                : new EmailConfirmation(entity.getConfirmationTokenHash(), entity.getConfirmationExpiresAt());
        return Account.restore(
                new AccountId(entity.getId()),
                entity.getName(),
                new Email(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getRole(),
                ZoneId.of(entity.getTimeZone()),
                entity.isEmailConfirmed(),
                confirmation,
                entity.getMailSentAt());
    }

    @Mapping(target = "confirmationTokenHash", ignore = true)
    @Mapping(target = "confirmationExpiresAt", ignore = true)
    @Mapping(target = "mailSentAt", ignore = true)
    void update(Account account, @MappingTarget AccountEntity entity);

    // MapStruct 1.6 cannot read Optional properties
    @AfterMapping
    default void updateOptionalFields(Account account, @MappingTarget AccountEntity entity) {
        entity.setConfirmationTokenHash(
                account.getConfirmation().map(EmailConfirmation::tokenHash).orElse(null));
        entity.setConfirmationExpiresAt(
                account.getConfirmation().map(EmailConfirmation::expiresAt).orElse(null));
        entity.setMailSentAt(account.getMailSentAt().orElse(null));
    }

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
