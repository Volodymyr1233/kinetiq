package com.kinetiq.fixture.domain.input;

/** Test-only use case port, placed where real ones live so the use case infrastructure picks it up. */
public interface PingUseCase {

    void ping();

    void fail();
}
