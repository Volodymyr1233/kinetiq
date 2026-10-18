package com.kinetiq.account.domain.output;

import com.kinetiq.account.domain.model.RawPassword;

public interface PasswordHasher {

    String hash(RawPassword password);
}
