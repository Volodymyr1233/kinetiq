package com.kinetiq.progress.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.progress.domain.input.Progress;
import com.kinetiq.progress.domain.input.Progress.ExerciseProgress;
import com.kinetiq.progress.domain.input.Progress.TopSetChange;
import com.kinetiq.progress.domain.input.ViewProgressUseCase;
import com.kinetiq.progress.domain.model.Period;
import com.kinetiq.progress.domain.model.TopSet;
import com.kinetiq.progress.domain.model.TrainingWeek;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ProgressController.class)
@Import({SecurityConfig.class, WebConfig.class, ProgressWebMapper.class})
class ProgressControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final String OWN = "/api/v1/progress?weeks=4";
    private static final String OF_CLIENT = "/api/v1/clients/" + CLIENT_ID.value() + "/progress?weeks=4";
    private static final Period FOUR_WEEKS = new Period(4);
    private static final RequestPostProcessor TRAINER =
            user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final Exercise SQUAT = new Exercise(
            ExerciseId.random(),
            TRAINER_ID,
            new LocalizedText("Back squat", "Przysiad"),
            Category.LEGS,
            new LocalizedText("", ""),
            Measure.REPS,
            3,
            5,
            90,
            new LocalizedText("", ""));
    private static final Exercise PLANK = new Exercise(
            ExerciseId.random(),
            TRAINER_ID,
            new LocalizedText("Plank", "Deska"),
            Category.CORE,
            new LocalizedText("", ""),
            Measure.SECONDS,
            3,
            45,
            60,
            new LocalizedText("", ""));

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ViewProgressUseCase progress;

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    private static TopSet squatSet(String loadKg) {
        return new TopSet(new BigDecimal(loadKg), 5, 8);
    }

    // week 4 of a plan, with a comparison of weeks 2 and 3 and every value present
    private static Progress progress() {
        LocalDate week3 = LocalDate.parse("2026-11-02");
        return new Progress(
                Goal.BUILD_MUSCLE,
                4,
                false,
                List.of(
                        new TrainingWeek(3, week3, 3, 3, false, 24, 3, Map.of()),
                        new TrainingWeek(4, week3.plusWeeks(1), 3, 1, true, 0, 0, Map.of())),
                4,
                6,
                new BigDecimal("8.0"),
                -1,
                new BigDecimal("0.3"),
                4,
                List.of(new ExerciseProgress(
                        SQUAT,
                        squatSet("105.0"),
                        new TopSetChange(new BigDecimal("2.5"), null),
                        true,
                        List.of(new ExerciseProgress.Week(3, squatSet("102.5"), new BigDecimal("7.7"))),
                        List.of(new ExerciseProgress.Recent(LocalDate.parse("2026-11-11"), squatSet("105.0"))))));
    }

    // week 1, nothing to compare, one exercise without a load
    private static Progress firstWeek() {
        TopSet plank = new TopSet(null, 45, 7);
        return new Progress(
                Goal.MOVE_BETTER,
                1,
                false,
                List.of(new TrainingWeek(1, LocalDate.parse("2026-11-09"), 3, 0, true, 0, 0, Map.of())),
                0,
                3,
                null,
                null,
                null,
                0,
                List.of(new ExerciseProgress(
                        PLANK,
                        plank,
                        null,
                        false,
                        List.of(new ExerciseProgress.Week(1, plank, new BigDecimal("7.0"))),
                        List.of(new ExerciseProgress.Recent(LocalDate.parse("2026-11-09"), plank)))));
    }

    @Nested
    class Own {

        @Test
        void shouldAnswerThePeriodItsWeeksAndItsExercises() {
            when(progress.own(CLIENT_ID, FOUR_WEEKS)).thenReturn(progress());

            var result = mvc.get().uri(OWN).with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            var json = assertThat(result).bodyJson();
            json.extractingPath("$.goal").isEqualTo("BUILD_MUSCLE");
            json.extractingPath("$.week").isEqualTo(4);
            json.extractingPath("$.finished").isEqualTo(false);
            json.extractingPath("$.sessionsDone").isEqualTo(4);
            json.extractingPath("$.sessionsPlanned").isEqualTo(6);
            json.extractingPath("$.sessionsDoneChange").isEqualTo(-1);
            json.extractingPath("$.averageRpe").isEqualTo(8.0);
            json.extractingPath("$.averageRpeChange").isEqualTo(0.3);
            json.extractingPath("$.weeksInARow").isEqualTo(4);
            json.extractingPath("$.weeks[0].number").isEqualTo(3);
            json.extractingPath("$.weeks[0].startsOn").isEqualTo("2026-11-02");
            json.extractingPath("$.weeks[0].planned").isEqualTo(3);
            json.extractingPath("$.weeks[0].done").isEqualTo(3);
            json.extractingPath("$.weeks[0].running").isEqualTo(false);
            json.extractingPath("$.weeks[0].averageRpe").isEqualTo(8.0);
            json.extractingPath("$.weeks[1].running").isEqualTo(true);
            json.extractingPath("$.exercises[0].exerciseId").isEqualTo(SQUAT.getId().value().toString());
            json.extractingPath("$.exercises[0].name").isEqualTo("Back squat");
            json.extractingPath("$.exercises[0].measure").isEqualTo("REPS");
            json.extractingPath("$.exercises[0].best.loadKg").isEqualTo(105.0);
            json.extractingPath("$.exercises[0].best.amount").isEqualTo(5);
            json.extractingPath("$.exercises[0].best.rpe").isEqualTo(8);
            json.extractingPath("$.exercises[0].topSetChange.loadKg").isEqualTo(2.5);
            json.extractingPath("$.exercises[0].prToday").isEqualTo(true);
            json.extractingPath("$.exercises[0].weeks[0].number").isEqualTo(3);
            json.extractingPath("$.exercises[0].weeks[0].top.loadKg").isEqualTo(102.5);
            json.extractingPath("$.exercises[0].weeks[0].averageRpe").isEqualTo(7.7);
            json.extractingPath("$.exercises[0].recent[0].date").isEqualTo("2026-11-11");
            json.extractingPath("$.exercises[0].recent[0].top.loadKg").isEqualTo(105.0);
        }

        @Test
        void shouldLeaveOutValuesThatAreAbsent() {
            when(progress.own(CLIENT_ID, FOUR_WEEKS)).thenReturn(firstWeek());

            var result = mvc.get().uri(OWN).with(CLIENT).exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            var json = assertThat(result).bodyJson();
            json.doesNotHavePath("$.averageRpe");
            json.doesNotHavePath("$.sessionsDoneChange");
            json.doesNotHavePath("$.averageRpeChange");
            json.doesNotHavePath("$.weeks[0].averageRpe");
            json.doesNotHavePath("$.exercises[0].topSetChange");
            json.doesNotHavePath("$.exercises[0].best.loadKg");
            json.extractingPath("$.exercises[0].measure").isEqualTo("SECONDS");
            json.extractingPath("$.exercises[0].best.amount").isEqualTo(45);
        }

        @Test
        void shouldRefuseAPeriodOtherThan4Or8Or12Weeks() {
            assertRefused(
                    mvc.get().uri("/api/v1/progress?weeks=5").with(CLIENT).exchange(),
                    HttpStatus.BAD_REQUEST,
                    "progress.period-invalid");
            verifyNoInteractions(progress);
        }

        @Test
        void shouldAnswerNotFound_withoutAPublishedPlan() {
            when(progress.own(CLIENT_ID, FOUR_WEEKS)).thenThrow(new PlanNotFoundException());

            assertRefused(mvc.get().uri(OWN).with(CLIENT).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");
        }

        @Test
        void shouldRefuseATrainerAndAVisitor() {
            assertRefused(mvc.get().uri(OWN).with(TRAINER).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
            assertRefused(mvc.get().uri(OWN).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
            verifyNoInteractions(progress);
        }
    }

    @Nested
    class OfClient {

        @Test
        void shouldAnswerTheClientsProgress_withNamesInTheRequestsLanguage() {
            when(progress.ofClient(TRAINER_ID, CLIENT_ID, FOUR_WEEKS)).thenReturn(progress());

            var result = mvc.get()
                    .uri(OF_CLIENT)
                    .header("Accept-Language", "pl")
                    .with(TRAINER)
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.OK);
            assertThat(result).bodyJson().extractingPath("$.week").isEqualTo(4);
            assertThat(result).bodyJson().extractingPath("$.exercises[0].name").isEqualTo("Przysiad");
        }

        @Test
        void shouldRefuseAPeriodOtherThan4Or8Or12Weeks() {
            assertRefused(
                    mvc.get()
                            .uri("/api/v1/clients/" + CLIENT_ID.value() + "/progress?weeks=6")
                            .with(TRAINER)
                            .exchange(),
                    HttpStatus.BAD_REQUEST,
                    "progress.period-invalid");
            verifyNoInteractions(progress);
        }

        @Test
        void shouldAnswerNotFound_forAClientWhoIsNotTheTrainersOwn() {
            when(progress.ofClient(TRAINER_ID, CLIENT_ID, FOUR_WEEKS)).thenThrow(new ClientNotFoundException());

            assertRefused(
                    mvc.get().uri(OF_CLIENT).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "client.not-found");
        }

        @Test
        void shouldAnswerNotFound_forAClientWithoutAPublishedPlan() {
            when(progress.ofClient(TRAINER_ID, CLIENT_ID, FOUR_WEEKS)).thenThrow(new PlanNotFoundException());

            assertRefused(mvc.get().uri(OF_CLIENT).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");
        }

        @Test
        void shouldRefuseAClientAndAVisitor() {
            assertRefused(
                    mvc.get().uri(OF_CLIENT).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
            assertRefused(mvc.get().uri(OF_CLIENT).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
            verifyNoInteractions(progress);
        }
    }
}
