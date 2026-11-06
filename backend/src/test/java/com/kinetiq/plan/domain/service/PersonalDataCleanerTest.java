package com.kinetiq.plan.domain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PersonalDataCleanerTest {

    private final PersonalDataCleaner cleaner = new PersonalDataCleaner("Anna Kowalski", "Marek Paweł Łukasz");

    @ParameterizedTest
    @ValueSource(
            strings = {
                "Anna", "anna", "ANNA", "Anny", "Annę", "Anne", "Annie", "Anną", "Annami", "Marek", "marek", "Marka",
                "Markiem", "Paweł", "Pawel", "PAWEL", "Pawła", "Pawla", "Pawłowi", "Łukasz", "Lukasz", "Łukasza",
                "Kowalski", "Kowalskiego", "Kowalskiemu", "Kowalskim"
            })
    void shouldReplaceTheWord_whenItIsAFormOfAName(String word) {
        assertThat(cleaner.clean("Ask " + word + " today")).isEqualTo("Ask [removed] today");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Joanna", "Annapolis", "Hanna", "Markowski", "Rozmarek", "Pawłowicz", "squat", "knee"})
    void shouldKeepTheWord_whenANameIsOnlyInsideALongerWord(String word) {
        assertThat(cleaner.clean("Ask " + word + " today")).isEqualTo("Ask " + word + " today");
    }

    @Test
    void shouldMatchOnlyExactly_whenTheStemIsShort() {
        PersonalDataCleaner shortName = new PersonalDataCleaner("Ola");

        assertThat(shortName.clean("Ola, OLA and Óla")).isEqualTo("[removed], [removed] and [removed]");
        assertThat(shortName.clean("Oli olej ol")).isEqualTo("Oli olej ol");
    }

    @Test
    void shouldLeaveOrdinaryWords_whenTheNameHoldsALoneInitial() {
        PersonalDataCleaner initial = new PersonalDataCleaner("A. Nowak");

        assertThat(initial.clean("a to i tamto, Nowak")).isEqualTo("a to i tamto, [removed]");
    }

    @Test
    void shouldReplaceEveryWordOfADoubleName() {
        PersonalDataCleaner doubleName = new PersonalDataCleaner("Anna-Maria Nowak-Kowalska");

        assertThat(doubleName.clean("Anna Maria, Nowak and Kowalskiej")).isEqualTo("[removed] [removed], [removed] and [removed]");
    }

    @ParameterizedTest
    @ValueSource(strings = {"jan.nowak@example.com", "JAN+training@mail.example.pl", "x_y%z@host"})
    void shouldReplaceTheAddress_whenShapedLikeAnEmail(String address) {
        assertThat(cleaner.clean("Write to " + address + " please")).isEqualTo("Write to [removed] please");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "600123456",
                "600 123 456",
                "600-123-456",
                "+48 600 123 456",
                "+48600123456",
                "(22) 123 45 67",
                "+48 (22) 123-45-67",
                "1234567"
            })
    void shouldReplaceTheNumber_whenShapedLikeAPhone(String phone) {
        assertThat(cleaner.clean("Call " + phone + " please")).isEqualTo("Call [removed] please");
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "Since 2019 I train",
                "Bench 120 kg, squat 140 kg",
                "3x10 at 82.5 kg",
                "120kg for 5 reps",
                "123456 is six digits",
                "Left knee, 2 surgeries"
            })
    void shouldKeepTheText_whenNumbersAreOrdinary(String text) {
        assertThat(cleaner.clean(text)).isEqualTo(text);
    }

    @Test
    void shouldKeepTheRestAsWritten_whenSomethingIsReplaced() {
        assertThat(cleaner.clean("Kolano boli, zapytaj Annę: anna@example.com, 600 123 456. Żadnych skoków!"))
                .isEqualTo("Kolano boli, zapytaj [removed]: [removed], [removed]. Żadnych skoków!");
    }
}
