package com.kinetiq.planchange.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.Language;
import com.kinetiq.messaging.domain.model.MessageText;
import com.kinetiq.planchange.domain.model.DeclineReason;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeclineMessageWordingTest {

    private static final String TITLE = "Back squat: cut volume by 13%";

    @ParameterizedTest
    @CsvSource({
        "TOO_EASY, too easy, za łatwy",
        "TOO_HARD, too hard, za trudny",
        "SCHEDULE, schedule, harmonogram",
        "INJURY, injury, kontuzja",
        "OTHER, other, inne"
    })
    void shouldNameTheTitleAndTheReason_inEachLanguage(DeclineReason reason, String english, String polish) {
        assertThat(DeclineMessageWording.of(Language.EN, TITLE, reason, null))
                .isEqualTo("Keeping the current plan instead of: " + TITLE + ". Reason: " + english + ".");
        assertThat(DeclineMessageWording.of(Language.PL, TITLE, reason, null))
                .isEqualTo("Zostaję przy obecnym planie zamiast: " + TITLE + ". Powód: " + polish + ".");
    }

    @Test
    void shouldPutTheNoteOnALineOfItsOwn_keepingItsLineBreaks() {
        String message = DeclineMessageWording.of(Language.EN, TITLE, DeclineReason.SCHEDULE, "Away that week.\nBack on the 23rd.");

        assertThat(message.lines().toList())
                .containsExactly(
                        "Keeping the current plan instead of: " + TITLE + ". Reason: schedule.",
                        "Away that week.",
                        "Back on the 23rd.");
    }

    @Test
    void shouldAddNoLine_whenThereIsNoNote() {
        assertThat(DeclineMessageWording.of(Language.PL, TITLE, DeclineReason.OTHER, null)).doesNotContain("\n");
    }

    @Test
    void shouldFitAMessage_withTheLongestTitleReasonAndNote() {
        String longTitle = "x".repeat(300);
        String longestNote = "y".repeat(500);

        for (Language language : Language.values()) {
            for (DeclineReason reason : DeclineReason.values()) {
                String text = DeclineMessageWording.of(language, longTitle, reason, longestNote);

                assertThat(text.length()).isLessThanOrEqualTo(MessageText.MAX_LENGTH);
                assertThat(new MessageText(text).value()).isEqualTo(text);
            }
        }
    }
}
