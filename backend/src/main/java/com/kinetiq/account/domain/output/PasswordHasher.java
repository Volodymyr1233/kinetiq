package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.RawPassword;

public interface PasswordHasher {

    String hash(RawPassword password);

    /** Compares a password as typed with a stored hash. The raw value is a plain string: it may break the rules. */
    boolean matches(String raw, String hash);
}
