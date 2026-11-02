package com.kinetiq.messaging.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.messaging.domain.exception.MessageEmptyException;
import com.kinetiq.messaging.domain.exception.MessageTooLongException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class MessageTextTest {

    private static final String EMOJI = "\uD83D\uDCAA";

    @Test
    void shouldRemoveSpacesAndLineBreaksAroundTheText_whenCreated() {
        assertThat(new MessageText(" \u00A0\n Is my knee ok? \r\n\u2003").value()).isEqualTo("Is my knee ok?");
    }

    @Test
    void shouldKeepLineBreaksInsideTheText() {
        assertThat(new MessageText("first\n\nsecond").value()).isEqualTo("first\n\nsecond");
    }

    @Test
    void shouldRefuseAsEmpty_whenNothingIsLeftAfterTrimming() {
        assertThatThrownBy(() -> new MessageText("")).isInstanceOf(MessageEmptyException.class);
        assertThatThrownBy(() -> new MessageText(" \n\t\u00A0 ")).isInstanceOf(MessageEmptyException.class);
    }

    // PostgreSQL refuses a NUL with an error, which would be a 500 for a harmless request
    @Test
    void shouldDropNulCharacters() {
        assertThat(new MessageText("a\0b").value()).isEqualTo("ab");
        assertThatThrownBy(() -> new MessageText("\0")).isInstanceOf(MessageEmptyException.class);
    }

    @Test
    void shouldAcceptTheLimitAndRefuseOneMore() {
        assertThat(new MessageText("a".repeat(2000)).value()).hasSize(2000);
        assertThatThrownBy(() -> new MessageText("a".repeat(2001))).isInstanceOf(MessageTooLongException.class);
    }

    // the limit is counted as the browser counts it
    @Test
    void shouldCountEmojiAsTwoUnits() {
        assertThat(new MessageText(EMOJI.repeat(1000)).value()).hasSize(2000);
        assertThatThrownBy(() -> new MessageText(EMOJI.repeat(1001))).isInstanceOf(MessageTooLongException.class);
    }

    @Test
    void shouldCountTheLimitAfterTrimming() {
        assertThat(new MessageText("  " + "a".repeat(2000) + "  ").value()).hasSize(2000);
    }

    @Test
    @Timeout(5)
    void shouldRefuseQuickly_whenAHugeTextHoldsALongRunOfSpaces() {
        String huge = " ".repeat(1_000_000) + "a" + " ".repeat(1_000_000) + "b";

        assertThatThrownBy(() -> new MessageText(huge)).isInstanceOf(MessageTooLongException.class);
    }

    @Test
    void shouldNeverPrintTheText() {
        assertThat(new MessageText("my knee hurts")).asString().doesNotContain("knee");
    }
}
