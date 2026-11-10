package com.kinetiq.planchange.domain.service;

import static com.kinetiq.planchange.domain.model.PlanSnapshots.entry;
import static com.kinetiq.planchange.domain.model.PlanSnapshots.timed;
import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.planchange.domain.exception.PlanChangeContentRejectedException;
import com.kinetiq.planchange.domain.model.DuePlanChange;
import com.kinetiq.planchange.domain.model.EntryRef;
import com.kinetiq.planchange.domain.model.Figure;
import com.kinetiq.planchange.domain.model.FigureKind;
import com.kinetiq.planchange.domain.model.PlanChangeContent;
import com.kinetiq.planchange.domain.model.PlanChangePromptMessages;
import com.kinetiq.planchange.domain.model.PlanSnapshot;
import com.kinetiq.planchange.domain.model.ProposedContent;
import com.kinetiq.planchange.domain.model.RemovedSession;
import com.kinetiq.planchange.domain.model.Rule;
import com.kinetiq.planchange.domain.model.SessionMove;
import com.kinetiq.planchange.domain.model.SessionSnapshot;
import com.kinetiq.planchange.domain.model.Target;
import com.kinetiq.planchange.domain.model.TargetChange;
import com.kinetiq.planchange.domain.model.WeekTraining;
import com.kinetiq.planchange.domain.rule.LimitCode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class PlanChangePromptTest {

    // what identifies the client or was written by a person: none of it may reach the service
    private static final String CLIENT_NAME = "Zofia Wiśniewska";
    private static final String CLIENT_EMAIL = "zofia.w@example.com";
    private static final AccountId CLIENT = new AccountId(UUID.fromString("7d0f5c1e-2b6a-4c55-9e1a-1f2e3d4c5b6a"));
    private static final ExerciseId SQUAT = new ExerciseId(UUID.fromString("a1b2c3d4-0000-4000-8000-00000000aaaa"));
    private static final ExerciseId PLANK = new ExerciseId(UUID.fromString("a1b2c3d4-0000-4000-8000-00000000bbbb"));
    private static final String MONDAY_NAME = "Zofia's heavy legs";
    private static final String FRIDAY_NAME = "Wiśniewska core finisher";

    private final PlanSnapshot plan = new PlanSnapshot(
            41,
            LocalDate.parse("2026-09-07"),
            12,
            List.of(
                    new SessionSnapshot(MONDAY, MONDAY_NAME, List.of(entry(SQUAT, 3, 8, "100.0"), timed(PLANK, 3, 45))),
                    new SessionSnapshot(WEDNESDAY, "Upper", List.of(entry(PLANK, 2, 30, null))),
                    new SessionSnapshot(FRIDAY, FRIDAY_NAME, List.of(entry(SQUAT, 4, 6, "110.0")))));

    private final DuePlanChange due = new DuePlanChange(
            CLIENT,
            ZoneId.of("Europe/Warsaw"),
            Rule.STALLED_WEIGHT,
            LocalDate.parse("2026-10-19"),
            LocalDate.parse("2026-11-08"),
            Set.of(SQUAT),
            List.of(
                    Figure.bestLoad(SQUAT, LocalDate.parse("2026-10-19"), new BigDecimal("100.000")),
                    Figure.bestLoad(SQUAT, LocalDate.parse("2026-10-26"), new BigDecimal("101.000")),
                    Figure.bestLoad(SQUAT, LocalDate.parse("2026-11-02"), new BigDecimal("102.500"))),
            List.of(new WeekTraining(LocalDate.parse("2026-11-02"), 3, 3, 2, List.of(), List.of(MONDAY, FRIDAY))));

    private final PlanChangePrompt prompt = new PlanChangePrompt(plan, due);

    @Test
    void shouldHoldNoNameEmailIdDateOrPersonWrittenText() {
        PlanChangePromptMessages messages = prompt.messages(Set.of(LimitCode.LOAD_OUT_OF_RANGE));
        String sent = String.join("\n", Stream.concat(
                        Stream.of(messages.system(), messages.user()), messages.rejections().stream())
                .toList());

        assertThat(sent)
                .doesNotContain(CLIENT_NAME, "Zofia", "Wiśniewska", CLIENT_EMAIL, "@")
                .doesNotContain(CLIENT.value().toString(), SQUAT.value().toString(), PLANK.value().toString())
                .doesNotContain(MONDAY_NAME, FRIDAY_NAME, "Upper")
                .doesNotContain("2026", "41");
    }

    @Test
    void shouldListThePlanByKeys_withTheRulesNumbersPerWeek() {
        String user = prompt.messages(Set.of()).user();

        assertThat(user)
                .contains("RULE: STALLED_WEIGHT")
                .contains("s1 MONDAY")
                .contains("s1e1 category=LEGS measure=REPS sets=3 amount=8 loadKg=100.0 rpe=8 restSeconds=90 concerned=true")
                .contains("s1e2 category=CORE measure=SECONDS sets=3 amount=45 loadKg=none")
                .contains("s3e1 ")
                .contains("week 1: bestLoadKg[s1e1,s3e1]=100")
                .contains("week 3: bestLoadKg[s1e1,s3e1]=102.5")
                .contains("finished per session over these weeks: s1=1 s2=0 s3=1");
    }

    @Test
    void shouldStateTheLimitsOfTheRule_andASentencePerRejection() {
        PlanChangePromptMessages messages =
                prompt.messages(List.of(LimitCode.LOAD_NOT_HALF_KG, LimitCode.LOAD_NOT_HALF_KG, LimitCode.UNKNOWN_KEY));

        assertThat(messages.system()).contains("by 2.5% to 15%, to a multiple of 0.5");
        assertThat(messages.rejections()).hasSize(2);
        assertThat(prompt.messages(Set.of()).rejections()).isEmpty();
    }

    @Test
    void shouldMapKeysBackToEntries_withTheOldValuesOfThePlan() {
        PlanChangeContent content = prompt.toContent(new ProposedContent(
                List.of(
                        new ProposedContent.Change("s1e1", null, 7, new BigDecimal("105.0")),
                        new ProposedContent.Change("s3e1", 4, null, new BigDecimal("115.5"))),
                Optional.empty(),
                List.of()));

        assertThat(content.changes())
                .containsExactlyInAnyOrder(
                        new TargetChange(new EntryRef(MONDAY, 0), SQUAT, MONDAY_NAME, Target.AMOUNT, BigDecimal.valueOf(8), BigDecimal.valueOf(7)),
                        new TargetChange(
                                new EntryRef(MONDAY, 0),
                                SQUAT,
                                MONDAY_NAME,
                                Target.LOAD_KG,
                                new BigDecimal("100.0"),
                                new BigDecimal("105.0")),
                        new TargetChange(
                                new EntryRef(FRIDAY, 0),
                                SQUAT,
                                FRIDAY_NAME,
                                Target.LOAD_KG,
                                new BigDecimal("110.0"),
                                new BigDecimal("115.5")));
    }

    @Test
    void shouldMapARemovedSessionAndItsMoves() {
        PlanChangeContent content = prompt.toContent(new ProposedContent(
                List.of(), Optional.of("s1"), List.of(new ProposedContent.Move("s1e2", "s3"))));

        assertThat(content.removed()).contains(new RemovedSession(MONDAY, MONDAY_NAME));
        assertThat(content.moves()).containsExactly(new SessionMove(new EntryRef(MONDAY, 1), FRIDAY));
    }

    private Set<String> rejected(ProposedContent proposed) {
        try {
            prompt.toContent(proposed);
            throw new AssertionError("accepted");
        } catch (PlanChangeContentRejectedException rejected) {
            return rejected.codes();
        }
    }

    @Test
    void shouldRefuseKeysThatAreNotInThePlan() {
        assertThat(rejected(new ProposedContent(
                        List.of(new ProposedContent.Change("s1e9", 3, null, null)), Optional.empty(), List.of())))
                .containsExactly(LimitCode.UNKNOWN_KEY);
        assertThat(rejected(new ProposedContent(List.of(), Optional.of("s7"), List.of())))
                .containsExactly(LimitCode.UNKNOWN_KEY);
        assertThat(rejected(new ProposedContent(
                        List.of(new ProposedContent.Change("bench press", 3, null, null)), Optional.empty(), List.of())))
                .containsExactly(LimitCode.UNKNOWN_KEY);
    }

    @Test
    void shouldRefuseALoadForAnEntryWithoutOne() {
        assertThat(rejected(new ProposedContent(
                        List.of(new ProposedContent.Change("s2e1", null, null, new BigDecimal("5.0"))),
                        Optional.empty(),
                        List.of())))
                .containsExactly(LimitCode.TARGET_NOT_ALLOWED);
    }

    @Test
    void shouldRefuseMovesThatAreNotFromTheRemovedSession_orTooMany() {
        assertThat(rejected(new ProposedContent(
                        List.of(), Optional.of("s1"), List.of(new ProposedContent.Move("s3e1", "s2")))))
                .containsExactly(LimitCode.MOVE_NOT_FROM_REMOVED);
        assertThat(rejected(new ProposedContent(
                        List.of(),
                        Optional.of("s1"),
                        List.of(
                                new ProposedContent.Move("s1e1", "s2"),
                                new ProposedContent.Move("s1e2", "s2"),
                                new ProposedContent.Move("s1e1", "s3")))))
                .containsExactly(LimitCode.TOO_MANY_MOVES);
    }

    @Test
    void shouldRefuseAnAnswerThatChangesNothing() {
        assertThat(rejected(new ProposedContent(
                        List.of(new ProposedContent.Change("s1e1", 3, 8, new BigDecimal("100"))), Optional.empty(), List.of())))
                .containsExactly(LimitCode.NOTHING_CHANGED);
    }

    @Test
    void shouldCountMeanRpeAndSessions_forTheOtherRules() {
        DuePlanChange lowCompletion = new DuePlanChange(
                CLIENT,
                ZoneId.of("Europe/Warsaw"),
                Rule.LOW_COMPLETION,
                LocalDate.parse("2026-10-26"),
                LocalDate.parse("2026-11-08"),
                Set.of(),
                List.of(
                        Figure.ofWeek(FigureKind.SESSIONS_PLANNED, LocalDate.parse("2026-10-26"), new BigDecimal("3.000")),
                        Figure.ofWeek(FigureKind.SESSIONS_FINISHED, LocalDate.parse("2026-10-26"), new BigDecimal("1.000")),
                        Figure.ofWeek(FigureKind.MEAN_RPE, LocalDate.parse("2026-10-26"), new BigDecimal("8.250"))),
                List.of());

        assertThat(new PlanChangePrompt(plan, lowCompletion).messages(Set.of()).user())
                .contains("week 1: sessionsPlanned=3 sessionsFinished=1 meanRpe=8.25");
    }
}
