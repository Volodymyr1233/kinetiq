package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.output.PasswordHasher;
import java.util.ArrayList;
import java.util.List;

/** Hashes {@code x} to {@code hashed:x} and records every hash and comparison. */
class RecordingPasswordHasher implements PasswordHasher {

    int calls;
    final List<String> comparedHashes = new ArrayList<>();

    @Override
    public String hash(RawPassword password) {
        calls++;
        return "hashed:" + password.value();
    }

    @Override
    public boolean matches(String raw, String hash) {
        comparedHashes.add(hash);
        return hash.equals("hashed:" + raw);
    }
}
