package com.kinetiq.account.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.util.Map;

/** A trainer's account can be deleted only once no client is linked to it. */
public class TrainerHasClientsException extends DomainException {

    private final int clients;

    public TrainerHasClientsException(int clients) {
        super("trainer still has clients");
        this.clients = clients;
    }

    @Override
    public String code() {
        return "account.has-clients";
    }

    @Override
    public Kind kind() {
        return Kind.CONFLICT;
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("clients", clients);
    }
}
