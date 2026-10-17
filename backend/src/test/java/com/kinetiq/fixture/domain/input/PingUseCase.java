package com.kinetiq.fixture.domain.input;

/** test-only use case port, placed where real ones live so use case infrastructure picks it up */
public interface PingUseCase {

    void ping();

    void fail();
}
