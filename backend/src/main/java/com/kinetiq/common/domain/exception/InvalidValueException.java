package com.kinetiq.common.domain.exception;

public class InvalidValueException extends DomainException {

    public InvalidValueException(String message) {
        super(message);
    }

    @Override
    public String code() {
        return "validation.failed";
    }
}
