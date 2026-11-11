package com.kinetiq.planchange.domain.service;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static org.assertj.core.api.Assertions.assertThat;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.PlanChangeState;
import com.kinetiq.planchange.domain.model.PlanChangeText;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PlanChangeWordingTest {

    private static final AccountId CLIENT = AccountId.random();
    private static final ExerciseId DEADLIFT = ExerciseId.random();
    private static final ExerciseId SQUAT = ExerciseId.random();
    private static final LocalDate WEEK_1 = LocalDate.parse("2026-10-19");
    private static final LocalDate WEEK_2 = LocalDate.parse("2026-10-26");
    private static final LocalDate WEEK_3 = LocalDate.parse("2026-11-02");

    private static final Map<ExerciseId, String> ENGLISH = Map.of(DEADLIFT, "Romanian deadlift", SQUAT, "Back squat");
    private static final Map<ExerciseId, String> POLISH =
            Map.of(DEADLIFT, "Martwy ciąg rumuński", SQUAT, "Przysiad ze sztangą");

    private static PlanChange planChange(
            Rule rule, PlanChangeContent content, List<Figure> figures, Set<ExerciseId> concerned) {
        return new PlanChange(
                PlanChangeId.random(),
                CLIENT,
                rule,
                PlanChangeState.PENDING,
                1,
                WEEK_2,
                LocalDate.parse("2026-11-08"),
                LocalDate.parse("2026-11-16"),
                content,
                figures,
                concerned,
                Instant.parse("2026-11-09T02:00:00Z"),
                null,
            null,
            null,
            null);
    }

    private static TargetChange change(ExerciseId exercise, int position, Target target, String from, String to) {
        return new TargetChange(
                new EntryRef(MONDAY, position), exercise, "Lower", target, new BigDecimal(from), new BigDecimal(to));
    }

    private static final PlanChange HIGH_RPE = planChange(
            Rule.HIGH_RPE,
            PlanChangeContent.ofChanges(List.of(change(DEADLIFT, 0, Target.AMOUNT, "10", "9"))),
            List.of(
                    Figure.ofWeek(FigureKind.MEAN_RPE, WEEK_2, new BigDecimal("8.333")),
                    Figure.ofWeek(FigureKind.MEAN_RPE, WEEK_3, new BigDecimal("8.550"))),
            Set.of(DEADLIFT));

    @Test
    void shouldNameTheExerciseAndTheRpeOfEachWeek_forHighRpe() {
        assertThat(PlanChangeWording.of(Language.EN, HIGH_RPE, ENGLISH))
                .isEqualTo(new PlanChangeText(
                        "Romanian deadlift: cut volume by 10%", "Average RPE was 8.3 and 8.6 in the last 2 weeks, above 8."));
        assertThat(PlanChangeWording.of(Language.PL, HIGH_RPE, POLISH))
                .isEqualTo(new PlanChangeText(
                        "Martwy ciąg rumuński: obniż objętość o 10%",
                        "Średnie RPE wynosiło 8,3 i 8,6 w ostatnich 2 tygodniach, powyżej 8."));
    }

    @Test
    void shouldCountTheExercises_andJudgeTheVolumeOfEachEntry_whenSeveralChange() {
        // the deadlift's volume drops 10% (4 x 10 to 3 x 12), the squat's by 12.5% (8 to 7): 11.25% rounds to 11
        PlanChange several = planChange(
                Rule.HIGH_RPE,
                PlanChangeContent.ofChanges(List.of(
                        change(DEADLIFT, 0, Target.SETS, "4", "3"),
                        change(DEADLIFT, 0, Target.AMOUNT, "10", "12"),
                        change(SQUAT, 1, Target.AMOUNT, "8", "7"))),
                HIGH_RPE.getFigures(),
                Set.of(DEADLIFT, SQUAT));

        assertThat(PlanChangeWording.of(Language.EN, several, ENGLISH).title()).isEqualTo("Cut volume on 2 exercises by 11%");
        assertThat(PlanChangeWording.of(Language.PL, several, POLISH).title())
                .isEqualTo("Obniż objętość w 2 ćwiczeniach o 11%");
    }

    @Test
    void shouldGiveTheChangeOfTheTotalVolume_whenTheVolumeOfEachEntryIsKnown() {
        // 4 x 10 = 40 to 36 and 10 x 8 = 80 to 70: 106 of 120 is 12% less, while the mean of 10% and 12.5% is 11%
        PlanChange several = planChange(
                Rule.HIGH_RPE,
                PlanChangeContent.ofChanges(List.of(
                        change(DEADLIFT, 0, Target.SETS, "4", "3"),
                        change(DEADLIFT, 0, Target.AMOUNT, "10", "12"),
                        change(SQUAT, 1, Target.AMOUNT, "8", "7"))),
                HIGH_RPE.getFigures(),
                Set.of(DEADLIFT, SQUAT));
        Map<EntryRef, Long> volumes = Map.of(new EntryRef(MONDAY, 0), 40L, new EntryRef(MONDAY, 1), 80L);

        assertThat(PlanChangeWording.of(Language.EN, several, ENGLISH, volumes).title())
                .isEqualTo("Cut volume on 2 exercises by 12%");
        assertThat(PlanChangeWording.of(Language.EN, several, ENGLISH, Map.of(new EntryRef(MONDAY, 0), 40L)).title())
                .isEqualTo("Cut volume on 2 exercises by 11%");
    }

    @Test
    void shouldNameTheRiseOfTheStalledExercise_forStalledWeight() {
        PlanChange stalled = planChange(
                Rule.STALLED_WEIGHT,
                PlanChangeContent.ofChanges(List.of(change(SQUAT, 0, Target.LOAD_KG, "100.0", "110.0"))),
                List.of(
                        Figure.bestLoad(SQUAT, WEEK_1, new BigDecimal("100.000")),
                        Figure.bestLoad(SQUAT, WEEK_2, new BigDecimal("101.000")),
                        Figure.bestLoad(SQUAT, WEEK_3, new BigDecimal("102.100"))),
                Set.of(SQUAT));

        assertThat(PlanChangeWording.of(Language.EN, stalled, ENGLISH))
                .isEqualTo(new PlanChangeText(
                        "Back squat: raise load by 10%", "Back squat rose 2.1% in 3 weeks, less than 5%."));
        assertThat(PlanChangeWording.of(Language.PL, stalled, POLISH))
                .isEqualTo(new PlanChangeText(
                        "Przysiad ze sztangą: podnieś obciążenie o 10%",
                        "Przysiad ze sztangą: wzrost o 2,1% w ciągu 3 tygodni, mniej niż 5%."));
    }

    @ParameterizedTest
    @CsvSource({"2, 2 ćwiczenia", "5, 5 ćwiczeń", "12, 12 ćwiczeń", "22, 22 ćwiczenia"})
    void shouldCountSeveralStalledExercises_withThePolishPlural(int count, String counted) {
        List<TargetChange> changes = new ArrayList<>();
        Set<ExerciseId> concerned = new HashSet<>();
        Map<ExerciseId, String> names = new HashMap<>();
        for (int i = 0; i < count; i++) {
            ExerciseId exercise = ExerciseId.random();
            concerned.add(exercise);
            names.put(exercise, "Exercise " + i);
            changes.add(change(exercise, i, Target.LOAD_KG, "100.0", "105.0"));
        }
        PlanChange stalled =
                planChange(Rule.STALLED_WEIGHT, PlanChangeContent.ofChanges(changes), List.of(
                        Figure.bestLoad(changes.getFirst().exercise(), WEEK_1, BigDecimal.TEN),
                        Figure.bestLoad(changes.getFirst().exercise(), WEEK_3, BigDecimal.TEN)), concerned);

        assertThat(PlanChangeWording.of(Language.PL, stalled, names))
                .isEqualTo(new PlanChangeText(
                        "Podnieś obciążenie w %d ćwiczeniach o 5%%".formatted(count),
                        counted + ": wzrost obciążenia poniżej 5% w ciągu 2 tygodni."));
        assertThat(PlanChangeWording.of(Language.EN, stalled, names))
                .isEqualTo(new PlanChangeText(
                        "Raise load on %d exercises by 5%%".formatted(count),
                        "%d exercises rose less than 5%% in 2 weeks.".formatted(count)));
    }

    @Test
    void shouldGiveTheSessionsLeftAndTheShareFinished_forLowCompletion() {
        PlanChange lowCompletion = planChange(
                Rule.LOW_COMPLETION,
                new PlanChangeContent(List.of(), Optional.of(new RemovedSession(SATURDAY, "Full body")), List.of()),
                List.of(
                        Figure.ofWeek(FigureKind.SESSIONS_PLANNED, WEEK_2, new BigDecimal("2.000")),
                        Figure.ofWeek(FigureKind.SESSIONS_FINISHED, WEEK_2, new BigDecimal("1.000")),
                        Figure.ofWeek(FigureKind.SESSIONS_PLANNED, WEEK_3, new BigDecimal("3.000")),
                        Figure.ofWeek(FigureKind.SESSIONS_FINISHED, WEEK_3, new BigDecimal("1.000"))),
                Set.of());

        // 2 of 5 is 40%; a full week plans 3 sessions, so 2 are left
        assertThat(PlanChangeWording.of(Language.EN, lowCompletion, Map.of()))
                .isEqualTo(new PlanChangeText(
                        "Cut to 2 sessions a week", "40% of planned sessions were finished in the last 2 weeks, below 70%."));
        assertThat(PlanChangeWording.of(Language.PL, lowCompletion, Map.of()))
                .isEqualTo(new PlanChangeText(
                        "Ograniczenie do 2 sesji w tygodniu",
                        "Ukończono 40% zaplanowanych sesji w ostatnich 2 tygodniach, poniżej 70%."));
    }

    @Test
    void shouldHoldNoIdentifier_andCountWithoutAName() {
        PlanChangeText text = PlanChangeWording.of(Language.EN, HIGH_RPE, Map.of());

        assertThat(text.title()).isEqualTo("Cut volume by 10%");
        assertThat(text.title() + text.reason())
                .doesNotContain(CLIENT.value().toString(), DEADLIFT.value().toString(), "@");
    }

    @Test
    void shouldPickThePolishPluralForm() {
        assertThat(List.of(1, 2, 4, 5, 11, 12, 14, 21, 22, 25, 112))
                .extracting(count -> PlanChangeWording.plural(count, "one", "few", "many"))
                .containsExactly("one", "few", "few", "many", "many", "many", "many", "many", "few", "many", "many");
    }
}
