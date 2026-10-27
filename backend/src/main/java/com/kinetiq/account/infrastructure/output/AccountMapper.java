package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.EmailConfirmation;
import com.kinetiq.account.domain.model.PasswordReset;
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
        PasswordReset passwordReset = entity.getResetTokenHash() == null
                ? null
                : new PasswordReset(entity.getResetTokenHash(), entity.getResetExpiresAt());
        return Account.restore(
                new AccountId(entity.getId()),
                entity.getName(),
                new Email(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getTrainerId() == null ? null : new AccountId(entity.getTrainerId()),
                ZoneId.of(entity.getTimeZone()),
                new AccountSettings(entity.getUnits(), entity.getLanguage(), entity.getTheme()),
                entity.isEmailConfirmed(),
                confirmation,
                entity.getMailSentAt(),
                passwordReset,
                entity.getHealthDataConsentAt());
    }

    @Mapping(target = "units", source = "settings.units")
    @Mapping(target = "language", source = "settings.language")
    @Mapping(target = "theme", source = "settings.theme")
    @Mapping(target = "trainerId", ignore = true)
    @Mapping(target = "confirmationTokenHash", ignore = true)
    @Mapping(target = "confirmationExpiresAt", ignore = true)
    @Mapping(target = "mailSentAt", ignore = true)
    @Mapping(target = "resetTokenHash", ignore = true)
    @Mapping(target = "resetExpiresAt", ignore = true)
    @Mapping(target = "healthDataConsentAt", ignore = true)
    void update(Account account, @MappingTarget AccountEntity entity);

    // MapStruct 1.6 cannot read Optional properties
    @AfterMapping
    default void updateOptionalFields(Account account, @MappingTarget AccountEntity entity) {
        entity.setTrainerId(account.getTrainerId().map(AccountId::value).orElse(null));
        entity.setConfirmationTokenHash(
                account.getConfirmation().map(EmailConfirmation::tokenHash).orElse(null));
        entity.setConfirmationExpiresAt(
                account.getConfirmation().map(EmailConfirmation::expiresAt).orElse(null));
        entity.setMailSentAt(account.getMailSentAt().orElse(null));
        entity.setHealthDataConsentAt(account.getHealthDataConsentAt().orElse(null));
        entity.setResetTokenHash(
                account.getPasswordReset().map(PasswordReset::tokenHash).orElse(null));
        entity.setResetExpiresAt(
                account.getPasswordReset().map(PasswordReset::expiresAt).orElse(null));
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
