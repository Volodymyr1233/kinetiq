package com.kinetiq.common.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTest {

    @Test
    void shouldTrimAndLowerCase_whenCreated() {
        assertThat(new Email(" Marek@Example.com ").value()).isEqualTo("marek@example.com");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "marek", "marek@example", "@example.com", "marek@.com", "ma rek@example.com", "a@b@c.d"})
    void shouldReject_whenMalformed(String value) {
        assertThatThrownBy(() -> new Email(value)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void shouldAccept_whenExactly254Characters() {
        assertThat(new Email(ofLength(254)).value()).hasSize(254);
    }

    @Test
    void shouldReject_when255Characters() {
        assertThatThrownBy(() -> new Email(ofLength(255))).isInstanceOf(InvalidValueException.class);
    }

    private static String ofLength(int length) {
        String domain = "@example.com";
        return "a".repeat(length - domain.length()) + domain;
    }
}
