package com.kinetiq.account.infrastructure.output;

import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.output.PasswordHasher;
import java.util.Map;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder.SecretKeyFactoryAlgorithm;
import org.springframework.stereotype.Component;

/** Uses PBKDF2 because bcrypt refuses passwords over 72 bytes. The {@code {pbkdf2}} prefix lets a later algorithm take over. */
@Component
class Pbkdf2PasswordHasher implements PasswordHasher {

    private static final String ID = "pbkdf2";

    // OWASP's current figure for PBKDF2-HMAC-SHA256
    private final PasswordEncoder encoder = new DelegatingPasswordEncoder(
            ID,
            Map.of(ID, new Pbkdf2PasswordEncoder("", 16, 600_000, SecretKeyFactoryAlgorithm.PBKDF2WithHmacSHA256)));

    @Override
    public String hash(RawPassword password) {
        return encoder.encode(password.value());
    }

    @Override
    public boolean matches(String raw, String hash) {
        return encoder.matches(raw, hash);
    }
}
