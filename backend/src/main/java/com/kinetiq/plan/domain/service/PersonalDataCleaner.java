package com.kinetiq.plan.domain.service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Takes known identifiers out of free text before it leaves the system: the words of the given names, also in Polish
 * inflected forms, and anything shaped like an id, an email address or a phone number.
 */
final class PersonalDataCleaner {

    static final String MARK = "[removed]";

    private static final Pattern PERSON_ID =
            Pattern.compile("\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b");
    private static final Pattern EMAIL = Pattern.compile("[\\p{L}\\p{N}._%+-]+@[\\p{L}\\p{N}.-]+");
    // 7 or more digits with optional +, spaces, dashes or brackets; a letter next to it makes it 3x10 or 120kg
    private static final Pattern PHONE =
            Pattern.compile("(?<![\\p{L}\\p{N}])\\+?\\(?\\d(?:[ \\t\\u00A0()-]*\\d){6,}\\)?(?![\\p{L}\\p{N}])");
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{M}]+");
    private static final Pattern NOT_A_LETTER = Pattern.compile("[^\\p{L}\\p{M}]+");
    private static final Pattern MARKS = Pattern.compile("\\p{M}+");
    private static final Pattern FINAL_VOWEL = Pattern.compile("[aeiouy]$");
    private static final Pattern FLEETING_E = Pattern.compile("e([^aeiouy])$");

    private static final int MIN_STEM = 3;
    private static final int MAX_ENDING = 3;
    // the masculine surname endings of Kowalski, longer than the rest
    private static final Set<String> LONG_ENDINGS = Set.of("iego", "iemu");

    private final List<Name> names;

    PersonalDataCleaner(String... names) {
        this.names = Stream.of(names)
                .flatMap(NOT_A_LETTER::splitAsStream)
                .map(PersonalDataCleaner::folded)
                // a lone initial would take out the Polish words "a" and "i"
                .filter(word -> word.length() > 1)
                .map(Name::of)
                .toList();
    }

    String clean(String text) {
        String withoutIds = PERSON_ID.matcher(text).replaceAll(MARK);
        String withoutContacts = PHONE.matcher(EMAIL.matcher(withoutIds).replaceAll(MARK)).replaceAll(MARK);
        return WORD.matcher(withoutContacts).replaceAll(word -> isName(word.group()) ? MARK : "$0");
    }

    private boolean isName(String word) {
        String folded = folded(word);
        return names.stream().anyMatch(name -> name.matches(folded));
    }

    private static String folded(String text) {
        String withoutMarks = MARKS.matcher(Normalizer.normalize(text, Normalizer.Form.NFD)).replaceAll("");
        // ł has no decomposition
        return withoutMarks.toLowerCase(Locale.ROOT).replace('ł', 'l');
    }

    // ponytail: stems catch regular Polish forms of stored names only; a third person's name, a nickname, or an
    // irregular form such as Ewa to Ewy stays, which constitution III accepts because the client was told
    private record Name(String word, List<String> stems) {

        static Name of(String word) {
            List<String> stems = new ArrayList<>();
            String stem = FINAL_VOWEL.matcher(word).replaceAll("");
            if (stem.length() >= MIN_STEM) {
                stems.add(stem);
            }
            String withoutFleeting = FLEETING_E.matcher(stem).replaceAll("$1");
            if (withoutFleeting.length() >= MIN_STEM && !withoutFleeting.equals(stem)) {
                stems.add(withoutFleeting);
            }
            return new Name(word, stems);
        }

        boolean matches(String other) {
            return other.equals(word) || stems.stream().anyMatch(stem -> isFormOf(other, stem));
        }

        private static boolean isFormOf(String other, String stem) {
            if (!other.startsWith(stem)) {
                return false;
            }
            String ending = other.substring(stem.length());
            return ending.length() <= MAX_ENDING || LONG_ENDINGS.contains(ending);
        }
    }
}
