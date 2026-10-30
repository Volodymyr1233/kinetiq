package com.kinetiq.client.infrastructure.input;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.input.ListClientsUseCase;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/clients")
@RequiredArgsConstructor
class ClientController {

    private final ListClientsUseCase clients;
    private final ClientWebMapper mapper;

    // sign-in stores the account id as the principal's name
    private static AccountId signedIn(Authentication authentication) {
        return new AccountId(UUID.fromString(authentication.getName()));
    }

    /** Lists the signed-in trainer's own clients, ordered by name. */
    @GetMapping
    ClientListResponse listClients(Authentication authentication) {
        return mapper.toListResponse(clients.listClients(signedIn(authentication)));
    }
}
