package com.kinetiq.account.domain.input;

/** Email and password as typed. Both stay plain strings: a malformed value is a wrong one, not an invalid request. */
public record SignInCommand(String email, String password) {

    @Override
    public String toString() {
        return "SignInCommand[***]";
    }
}
