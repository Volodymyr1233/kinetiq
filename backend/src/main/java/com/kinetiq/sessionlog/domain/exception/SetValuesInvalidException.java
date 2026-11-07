package com.kinetiq.sessionlog.domain.exception;

import com.kinetiq.common.domain.exception.DomainException;
import java.util.Map;

/** A set was sent with a value outside its limits, or at a place the session does not have. */
public class SetValuesInvalidException extends DomainException {

    private final String field;

    public SetValuesInvalidException(String field) {
        super("set value invalid: " + field);
        this.field = field;
    }

    @Override
    public String code() {
        return "set-values.invalid";
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("field", field);
    }
}
