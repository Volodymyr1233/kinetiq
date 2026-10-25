package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.UpdateAccountCommand;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Email;
import java.time.ZoneId;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface AccountWebMapper {

    @Mapping(target = "units", source = "settings.units")
    @Mapping(target = "language", source = "settings.language")
    @Mapping(target = "theme", source = "settings.theme")
    CurrentAccountResponse toResponse(Account account);

    UpdateAccountCommand toCommand(UpdateAccountRequest request);

    default UUID map(AccountId id) {
        return id.value();
    }

    default String map(ZoneId zone) {
        return zone.getId();
    }

    default String map(Email email) {
        return email.value();
    }
}
