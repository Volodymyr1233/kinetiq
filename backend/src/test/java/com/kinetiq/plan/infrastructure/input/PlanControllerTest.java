package com.kinetiq.plan.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.client.domain.exception.ClientNotFoundException;
import com.kinetiq.common.domain.exception.DomainException;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.model.Category;
import com.kinetiq.exercise.domain.model.Exercise;
import com.kinetiq.exercise.domain.model.ExerciseId;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.exercise.domain.model.Measure;
import com.kinetiq.plan.domain.exception.PlanAlreadyPublishedException;
import com.kinetiq.plan.domain.exception.PlanChangedException;
import com.kinetiq.plan.domain.exception.PlanExerciseNotInLibraryException;
import com.kinetiq.plan.domain.exception.PlanNotFoundException;
import com.kinetiq.plan.domain.exception.PlanDraftingInProgressException;
import com.kinetiq.plan.domain.exception.PlanQuizNotSubmittedException;
import com.kinetiq.plan.domain.exception.PlanRuleViolationException;
import com.kinetiq.plan.domain.exception.PlanWeeksBeforeCurrentException;
import com.kinetiq.plan.domain.input.ManagePlanUseCase;
import com.kinetiq.plan.domain.input.PlanDetails;
import com.kinetiq.plan.domain.input.SavePlanCommand;
import com.kinetiq.plan.domain.model.ExerciseEntry;
import com.kinetiq.plan.domain.model.Plan;
import com.kinetiq.plan.domain.model.PlanId;
import com.kinetiq.plan.domain.model.Session;
import com.kinetiq.quiz.domain.model.Goal;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(PlanController.class)
@Import({SecurityConfig.class, WebConfig.class, PlanWebMapperImpl.class})
class PlanControllerTest {

    private static final AccountId TRAINER_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final String ADDRESS = "/api/v1/clients/" + CLIENT_ID.value() + "/plan";
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT =
            user(CLIENT_ID.value().toString()).roles("CLIENT");

    private static final Exercise SQUAT = new Exercise(
            new ExerciseId(UUID.fromString("00000000-0000-4000-9100-000000000011")),
            null,
            new LocalizedText("Back squat", "Przysiad ze sztangą"),
            Category.LEGS,
            new LocalizedText("", ""),
            Measure.REPS,
            3,
            5,
            120,
            new LocalizedText("", ""));

    private static final String ENTRY =
            """
            {"exerciseId":"00000000-0000-4000-9100-000000000011","sets":3,"amount":5,"loadKg":100.5,"rpe":8,"restSeconds":120}
            """;
    private static final String BODY =
            """
            {"version":4,"goal":"BUILD_MUSCLE","weeks":16,
             "sessions":[{"name":"Lower body","day":"WEDNESDAY","entries":[%s]}]}
            """
                    .formatted(ENTRY);

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ManagePlanUseCase plans;

    private static PlanDetails details(LocalDate firstWeek, Integer currentWeek, BigDecimal load) {
        Session session = new Session(
                "Lower body",
                DayOfWeek.WEDNESDAY,
                List.of(new ExerciseEntry(SQUAT.getId(), 3, 5, load, 8, 120)));
        Plan plan = new Plan(PlanId.random(), CLIENT_ID, Goal.BUILD_MUSCLE, 16, firstWeek, List.of(session), 4);
        return new PlanDetails(plan, currentWeek, Map.of(SQUAT.getId(), SQUAT));
    }

    private MvcTestResult put(String body) {
        return mvc.put()
                .uri(ADDRESS)
                .with(TRAINER)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    private void assertRefused(MvcTestResult result, HttpStatus status, String code) {
        assertThat(result).hasStatus(status);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo(code);
    }

    @Test
    void shouldAnswerTheMark_whenTheDraftCarriesOne() {
        Plan failed = Plan.failed(CLIENT_ID, Goal.BUILD_MUSCLE, 12);
        when(plans.get(TRAINER_ID, CLIENT_ID)).thenReturn(new PlanDetails(failed, null, Map.of()));

        assertThat(mvc.get().uri(ADDRESS).with(TRAINER))
                .bodyJson()
                .extractingPath("$.automaticDraft")
                .isEqualTo("FAILED");
    }

    @Test
    void shouldLeaveTheMarkOut_whenThePlanCarriesNone() {
        when(plans.get(TRAINER_ID, CLIENT_ID)).thenReturn(details(null, null, null));

        assertThat(mvc.get().uri(ADDRESS).with(TRAINER)).bodyJson().doesNotHavePath("$.automaticDraft");
    }

    @Test
    void shouldAnswerEveryField_whenPublished() {
        when(plans.get(TRAINER_ID, CLIENT_ID))
                .thenReturn(details(LocalDate.parse("2026-10-26"), 2, new BigDecimal("100.0")));

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");
        assertThat(result).bodyJson().extractingPath("$.goal").isEqualTo("BUILD_MUSCLE");
        assertThat(result).bodyJson().extractingPath("$.weeks").isEqualTo(16);
        assertThat(result).bodyJson().extractingPath("$.firstWeek").isEqualTo("2026-10-26");
        assertThat(result).bodyJson().extractingPath("$.currentWeek").isEqualTo(2);
        assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(4);
        assertThat(result).bodyJson().extractingPath("$.sessions[0].name").isEqualTo("Lower body");
        assertThat(result).bodyJson().extractingPath("$.sessions[0].day").isEqualTo("WEDNESDAY");
        String entry = "$.sessions[0].entries[0]";
        assertThat(result).bodyJson().extractingPath(entry + ".exerciseId").isEqualTo(SQUAT.getId().value().toString());
        assertThat(result).bodyJson().extractingPath(entry + ".exerciseName").isEqualTo("Back squat");
        assertThat(result).bodyJson().extractingPath(entry + ".measure").isEqualTo("REPS");
        assertThat(result).bodyJson().extractingPath(entry + ".sets").isEqualTo(3);
        assertThat(result).bodyJson().extractingPath(entry + ".amount").isEqualTo(5);
        assertThat(result).bodyJson().extractingPath(entry + ".loadKg").isEqualTo(100.0);
        assertThat(result).bodyJson().extractingPath(entry + ".rpe").isEqualTo(8);
        assertThat(result).bodyJson().extractingPath(entry + ".restSeconds").isEqualTo(120);
    }

    @Test
    void shouldLeaveOutWhatIsEmpty_whenDraft() {
        when(plans.get(TRAINER_ID, CLIENT_ID)).thenReturn(details(null, null, null));

        var result = mvc.get().uri(ADDRESS).with(TRAINER).exchange();

        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("DRAFT");
        assertThat(result).bodyJson().doesNotHavePath("$.firstWeek");
        assertThat(result).bodyJson().doesNotHavePath("$.currentWeek");
        assertThat(result).bodyJson().doesNotHavePath("$.sessions[0].entries[0].loadKg");
    }

    @Test
    void shouldNameExercisesInTheLanguageOfTheRequest() {
        when(plans.get(TRAINER_ID, CLIENT_ID)).thenReturn(details(null, null, null));

        var result = mvc.get().uri(ADDRESS).header("Accept-Language", "pl").with(TRAINER).exchange();

        assertThat(result).bodyJson().extractingPath("$.sessions[0].entries[0].exerciseName")
                .isEqualTo("Przysiad ze sztangą");
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertRefused(mvc.get().uri(ADDRESS).exchange(), HttpStatus.UNAUTHORIZED, "request.unauthenticated");
        assertRefused(
                mvc.put().uri(ADDRESS).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(BODY).exchange(),
                HttpStatus.UNAUTHORIZED,
                "request.unauthenticated");
        verifyNoInteractions(plans);
    }

    // a client's own view of the plan is another story, so they are refused even for their own plan
    @Test
    void shouldRefuse_whenCallerIsClient() {
        assertRefused(mvc.get().uri(ADDRESS).with(CLIENT).exchange(), HttpStatus.FORBIDDEN, "request.not-allowed");
        assertRefused(
                mvc.put()
                        .uri(ADDRESS)
                        .with(CLIENT)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .exchange(),
                HttpStatus.FORBIDDEN,
                "request.not-allowed");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldRefuseASave_whenTheCsrfTokenIsMissing() {
        var result = mvc.put().uri(ADDRESS).with(TRAINER).contentType(MediaType.APPLICATION_JSON).content(BODY).exchange();

        assertRefused(result, HttpStatus.FORBIDDEN, "request.forbidden");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldAnswerNotFound_whenTheClientIsUnknownOrAnotherTrainers() {
        when(plans.get(TRAINER_ID, CLIENT_ID)).thenThrow(new ClientNotFoundException());
        when(plans.save(any())).thenThrow(new ClientNotFoundException());

        assertRefused(mvc.get().uri(ADDRESS).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "client.not-found");
        assertRefused(put(BODY), HttpStatus.NOT_FOUND, "client.not-found");
    }

    @Test
    void shouldTellNoPlanApartFromAForeignClient() {
        when(plans.get(TRAINER_ID, CLIENT_ID)).thenThrow(new PlanNotFoundException());

        assertRefused(mvc.get().uri(ADDRESS).with(TRAINER).exchange(), HttpStatus.NOT_FOUND, "plan.not-found");
    }

    @Test
    void shouldAnswerInvalid_whenTheIdIsNoUuid() {
        assertRefused(
                mvc.get().uri("/api/v1/clients/not-an-id/plan").with(TRAINER).exchange(),
                HttpStatus.BAD_REQUEST,
                "request.invalid");
    }

    @Test
    void shouldSaveThePlan_passingTheSignedInTrainerAndClient() {
        when(plans.save(any())).thenReturn(details(null, null, null));

        var result = put(BODY);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.version").isEqualTo(4);
        ArgumentCaptor<SavePlanCommand> command = ArgumentCaptor.forClass(SavePlanCommand.class);
        verify(plans).save(command.capture());
        assertThat(command.getValue().trainer()).isEqualTo(TRAINER_ID);
        assertThat(command.getValue().client()).isEqualTo(CLIENT_ID);
        assertThat(command.getValue().version()).isEqualTo(4L);
        assertThat(command.getValue().goal()).isEqualTo(Goal.BUILD_MUSCLE);
        assertThat(command.getValue().weeks()).isEqualTo(16);
        Session session = command.getValue().sessions().getFirst();
        assertThat(session.name()).isEqualTo("Lower body");
        assertThat(session.day()).isEqualTo(DayOfWeek.WEDNESDAY);
        ExerciseEntry entry = session.entries().getFirst();
        assertThat(entry.exercise()).isEqualTo(SQUAT.getId());
        assertThat(entry.loadKg()).isEqualByComparingTo("100.5");
        assertThat(entry.restSeconds()).isEqualTo(120);
    }

    @Test
    void shouldAcceptAFirstSave_withoutAVersionOrSessions() {
        when(plans.save(any())).thenReturn(details(null, null, null));

        var result = put("""
                {"goal":"LOSE_FAT","weeks":12,"sessions":[]}
                """);

        assertThat(result).hasStatus(HttpStatus.OK);
        ArgumentCaptor<SavePlanCommand> command = ArgumentCaptor.forClass(SavePlanCommand.class);
        verify(plans).save(command.capture());
        assertThat(command.getValue().version()).isNull();
    }

    private static String withEntryField(String field, String value) {
        return BODY.replaceFirst("\"" + field + "\":[^,}]+", "\"" + field + "\":" + value);
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
        "sets, 0, min",
        "sets, 21, max",
        "sets, null, not-null",
        "amount, 0, min",
        "amount, 3601, max",
        "rpe, 4, min",
        "rpe, 11, max",
        "restSeconds, -1, min",
        "restSeconds, 601, max",
        "restSeconds, null, not-null",
        "loadKg, 0, decimal-min",
        "loadKg, 1000, digits",
        "loadKg, 62.55, digits"
    })
    void shouldNameTheField_whenATargetIsInvalid(String field, String value, String code) {
        var result = put(withEntryField(field, value));

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field")
                .isEqualTo("sessions[0].entries[0]." + field);
        assertThat(result).bodyJson().extractingPath("$.errors[0].code").isEqualTo(code);
        verifyNoInteractions(plans);
    }

    @ParameterizedTest
    @CsvSource({"3, min", "17, max"})
    void shouldNameTheLength_whenItIsOutOfRange(int weeks, String code) {
        var result = put("""
                {"goal":"LOSE_FAT","weeks":%d,"sessions":[]}
                """.formatted(weeks));

        assertRefused(result, HttpStatus.BAD_REQUEST, "validation.failed");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("weeks");
        assertThat(result).bodyJson().extractingPath("$.errors[0].code").isEqualTo(code);
    }

    @Test
    void shouldNameTheSessionsName_whenItIsBlankOrTooLong() {
        var blank = put(BODY.replace("Lower body", "   "));
        var tooLong = put(BODY.replace("Lower body", "x".repeat(61)));

        assertThat(blank).bodyJson().extractingPath("$.errors[0].field").isEqualTo("sessions[0].name");
        assertThat(blank).bodyJson().extractingPath("$.errors[0].code").isEqualTo("not-blank");
        assertThat(tooLong).bodyJson().extractingPath("$.errors[0].code").isEqualTo("size");
    }

    // a request stays bounded before anything is done with it
    @Test
    void shouldRefuse_whenAListIsTooLong() {
        String session = "{\"name\":\"A\",\"day\":\"MONDAY\",\"entries\":[]}";
        String sessions = String.join(",", Collections.nCopies(7, session));
        String entries = String.join(",", Collections.nCopies(31, ENTRY));

        var tooManySessions = put("""
                {"goal":"LOSE_FAT","weeks":12,"sessions":[%s]}
                """.formatted(sessions));
        var tooManyEntries = put("""
                {"goal":"LOSE_FAT","weeks":12,"sessions":[{"name":"A","day":"MONDAY","entries":[%s]}]}
                """.formatted(entries));

        assertThat(tooManySessions).bodyJson().extractingPath("$.errors[0].field").isEqualTo("sessions");
        assertThat(tooManyEntries).bodyJson().extractingPath("$.errors[0].field").isEqualTo("sessions[0].entries");
        verifyNoInteractions(plans);
    }

    @ParameterizedTest
    @CsvSource(
            delimiter = '|',
            value = {
                "{\"goal\":\"LOSE_FAT\",\"weeks\":12,\"sessions\":[{\"name\":\"A\",\"day\":\"FUNDAY\",\"entries\":[]}]}",
                "{\"goal\":\"LOSE_FAT\",\"weeks\":\"twelve\",\"sessions\":[]}",
                "{\"goal\":\"NAP\",\"weeks\":12,\"sessions\":[]}",
                "not json"
            })
    void shouldRefuseTheWholeRequest_whenItCannotBeRead(String body) {
        assertRefused(put(body), HttpStatus.BAD_REQUEST, "request.invalid");
        verifyNoInteractions(plans);
    }

    private static Stream<Arguments> refusals() {
        return Stream.of(
                Arguments.of(new PlanChangedException(), HttpStatus.CONFLICT, "plan.changed"),
                Arguments.of(new PlanQuizNotSubmittedException(), HttpStatus.CONFLICT, "plan.quiz-not-submitted"),
                Arguments.of(new PlanDraftingInProgressException(), HttpStatus.CONFLICT, "plan.drafting"),
                Arguments.of(
                        new PlanExerciseNotInLibraryException(),
                        HttpStatus.BAD_REQUEST,
                        "plan.exercise-not-in-library"),
                Arguments.of(
                        new PlanRuleViolationException("plan.duplicate-day"),
                        HttpStatus.BAD_REQUEST,
                        "plan.duplicate-day"),
                Arguments.of(
                        new PlanRuleViolationException("plan.too-few-sessions"),
                        HttpStatus.BAD_REQUEST,
                        "plan.too-few-sessions"),
                Arguments.of(
                        new PlanRuleViolationException("plan.session-without-exercises"),
                        HttpStatus.BAD_REQUEST,
                        "plan.session-without-exercises"));
    }

    @ParameterizedTest
    @MethodSource("refusals")
    void shouldAnswerEachRefusal_withItsStatusAndCodeAndNoInternalText(
            DomainException refusal, HttpStatus status, String code) {
        when(plans.save(any())).thenThrow(refusal);

        var result = put(BODY);

        assertRefused(result, status, code);
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
        assertThat(result).bodyText().doesNotContain(refusal.getMessage()).doesNotContain("Exception");
    }

    private MvcTestResult publish(String body) {
        return mvc.post()
                .uri(ADDRESS + "/publication")
                .with(TRAINER)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Test
    void shouldPublish_passingTheVersionAndAnsweringThePlan() {
        when(plans.publish(TRAINER_ID, CLIENT_ID, 4)).thenReturn(details(LocalDate.parse("2026-11-02"), 1, null));

        var result = publish("""
                {"version":4}
                """);

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.status").isEqualTo("PUBLISHED");
        assertThat(result).bodyJson().extractingPath("$.firstWeek").isEqualTo("2026-11-02");
        assertThat(result).bodyJson().extractingPath("$.currentWeek").isEqualTo(1);
    }

    @Test
    void shouldRefuseAPublish_whenTheVersionIsMissing() {
        var result = publish("{}");

        assertRefused(result, HttpStatus.BAD_REQUEST, "validation.failed");
        assertThat(result).bodyJson().extractingPath("$.errors[0].field").isEqualTo("version");
        verifyNoInteractions(plans);
    }

    private static Stream<Arguments> publishRefusals() {
        return Stream.of(
                Arguments.of(new ClientNotFoundException(), HttpStatus.NOT_FOUND, "client.not-found"),
                Arguments.of(new PlanNotFoundException(), HttpStatus.NOT_FOUND, "plan.not-found"),
                Arguments.of(new PlanAlreadyPublishedException(), HttpStatus.CONFLICT, "plan.already-published"),
                Arguments.of(new PlanChangedException(), HttpStatus.CONFLICT, "plan.changed"),
                Arguments.of(
                        new PlanRuleViolationException("plan.too-few-sessions"),
                        HttpStatus.BAD_REQUEST,
                        "plan.too-few-sessions"),
                Arguments.of(
                        new PlanRuleViolationException("plan.session-without-exercises"),
                        HttpStatus.BAD_REQUEST,
                        "plan.session-without-exercises"));
    }

    @ParameterizedTest
    @MethodSource("publishRefusals")
    void shouldAnswerEachPublishRefusal_withItsStatusAndCode(DomainException refusal, HttpStatus status, String code) {
        when(plans.publish(TRAINER_ID, CLIENT_ID, 4)).thenThrow(refusal);

        var result = publish("""
                {"version":4}
                """);

        assertRefused(result, status, code);
        assertThat(result).bodyText().doesNotContain(refusal.getMessage());
    }

    @Test
    void shouldRefuseAPublish_byRoleAndWithoutASignIn() {
        String address = ADDRESS + "/publication";
        String body = "{\"version\":4}";

        assertRefused(
                mvc.post().uri(address).with(CLIENT).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body).exchange(),
                HttpStatus.FORBIDDEN,
                "request.not-allowed");
        assertRefused(
                mvc.post().uri(address).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body).exchange(),
                HttpStatus.UNAUTHORIZED,
                "request.unauthenticated");
        verifyNoInteractions(plans);
    }

    @Test
    void shouldGiveTheCurrentWeek_whenShortenedBelowIt() {
        when(plans.save(any())).thenThrow(new PlanWeeksBeforeCurrentException(6));

        var result = put(BODY);

        assertRefused(result, HttpStatus.BAD_REQUEST, "plan.weeks-before-current");
        assertThat(result).bodyJson().extractingPath("$.currentWeek").isEqualTo(6);
    }
}
