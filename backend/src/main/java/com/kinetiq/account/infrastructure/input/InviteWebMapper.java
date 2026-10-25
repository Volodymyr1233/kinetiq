package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.input.CreatedInvite;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.common.domain.model.Email;
import java.util.List;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface InviteWebMapper {

    InviteResponse toResponse(Invite invite);

    CreatedInviteResponse toResponse(CreatedInvite created);

    default InviteListResponse toListResponse(List<Invite> invites) {
        return new InviteListResponse(invites.stream().map(this::toResponse).toList());
    }

    default UUID map(InviteId id) {
        return id.value();
    }

    default String map(Email email) {
        return email.value();
    }
}
