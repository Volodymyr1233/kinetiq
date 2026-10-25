package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.common.domain.model.Email;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface InviteMapper {

    // the aggregate has no setters, so this direction goes through its restore factory
    default Invite toDomain(InviteEntity entity) {
        return Invite.restore(
                new InviteId(entity.getId()),
                new AccountId(entity.getTrainerId()),
                new Email(entity.getEmail()),
                entity.getStatus(),
                entity.getEmailTokenHash(),
                entity.getLinkTokenHash(),
                entity.getSentAt(),
                entity.getExpiresAt());
    }

    void update(Invite invite, @MappingTarget InviteEntity entity);

    default UUID map(InviteId id) {
        return id.value();
    }

    default UUID map(AccountId id) {
        return id.value();
    }

    default String map(Email email) {
        return email.value();
    }
}
