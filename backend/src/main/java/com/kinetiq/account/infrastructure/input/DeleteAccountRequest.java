package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotBlank;

record DeleteAccountRequest(@NotBlank String password) {

    @Override
    public String toString() {
        return "DeleteAccountRequest[***]";
    }
}
