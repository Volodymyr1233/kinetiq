package com.kinetiq.readiness.domain.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.common.domain.exception.InvalidValueException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AnswersTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 6})
    void shouldRefuseAnAnswerOutsideOneToFive_forEachQuestion(int value) {
        assertThatThrownBy(() -> new Answers(value, 3, 3)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Answers(3, value, 3)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Answers(3, 3, value)).isInstanceOf(InvalidValueException.class);
    }
}
