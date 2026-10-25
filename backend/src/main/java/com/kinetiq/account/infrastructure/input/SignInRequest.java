package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.NotBlank;

// only emptiness is checked: any other wrong value gets the wrong-email-or-password answer
record SignInRequest(@NotBlank String email, @NotBlank String password) {

    @Override
    public String toString() {
        return "SignInRequest[***]";
    }
}
