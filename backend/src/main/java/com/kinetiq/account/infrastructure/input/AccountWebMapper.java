package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import java.time.ZoneId;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface AccountWebMapper {

    CurrentAccountResponse toResponse(Account account);

    default UUID map(AccountId id) {
        return id.value();
    }

    default String map(ZoneId zone) {
        return zone.getId();
    }
}
