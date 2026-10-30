package com.kinetiq.client.infrastructure.input;

import com.kinetiq.client.domain.model.ClientSummary;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
interface ClientWebMapper {

    default ClientResponse toResponse(ClientSummary client) {
        return new ClientResponse(
                client.getId().value(),
                client.getName(),
                client.getGoal().orElse(null),
                client.getPlanState());
    }

    default ClientListResponse toListResponse(List<ClientSummary> clients) {
        return new ClientListResponse(clients.stream().map(this::toResponse).toList());
    }
}
