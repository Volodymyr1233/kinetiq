package com.kinetiq.messaging.infrastructure.input;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.infrastructure.config.SecurityConfig;
import com.kinetiq.common.infrastructure.config.WebConfig;
import com.kinetiq.exercise.domain.model.LocalizedText;
import com.kinetiq.messaging.domain.exception.AboutSetNotFoundException;
import com.kinetiq.messaging.domain.exception.ConversationNotFoundException;
import com.kinetiq.messaging.domain.exception.MessageEmptyException;
import com.kinetiq.messaging.domain.exception.MessageIdInUseException;
import com.kinetiq.messaging.domain.exception.MessageNotFoundException;
import com.kinetiq.messaging.domain.exception.MessageTooLongException;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.messaging.domain.model.AboutSet;
import com.kinetiq.messaging.domain.model.ConversationId;
import com.kinetiq.messaging.domain.model.ConversationOverview;
import com.kinetiq.messaging.domain.model.LastMessage;
import com.kinetiq.messaging.domain.model.Message;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.messaging.domain.model.MessagePage;
import com.kinetiq.messaging.domain.model.MessageText;
import com.kinetiq.messaging.domain.model.PlanWeek;
import com.kinetiq.quiz.domain.model.Goal;
import com.kinetiq.sessionlog.domain.model.SessionLogId;
import com.kinetiq.sessionlog.domain.model.SetLogId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(ConversationController.class)
@Import({SecurityConfig.class, WebConfig.class, MessagingWebMapperImpl.class})
class ConversationControllerTest {

    private static final AccountId TRAINER_ID =
            new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000001"));
    private static final AccountId CLIENT_ID = new AccountId(UUID.fromString("00000000-0000-4000-8000-000000000003"));
    private static final RequestPostProcessor TRAINER = user(TRAINER_ID.value().toString()).roles("TRAINER");
    private static final RequestPostProcessor CLIENT = user(CLIENT_ID.value().toString()).roles("CLIENT");
    private static final UUID MESSAGE_ID = UUID.fromString("00000000-0000-4000-8000-000000000701");
    private static final UUID SET_LOG_ID = UUID.fromString("00000000-0000-4000-8000-000000000801");
    private static final UUID SESSION_LOG_ID = UUID.fromString("00000000-0000-4000-8000-000000000901");
    private static final Instant AT = Instant.parse("2026-11-02T09:12:00Z");
    private static final String MESSAGES = "/api/v1/conversations/" + CLIENT_ID.value() + "/messages";
    private static final String READ = "/api/v1/conversations/" + CLIENT_ID.value() + "/read";
    private static final String BODY = "{\"id\":\"" + MESSAGE_ID + "\",\"text\":\"hello\"}";

    @Autowired
    private MockMvcTester mvc;

    @MockitoBean
    private ExchangeMessagesUseCase messaging;

    private static Message message(AccountId sender) {
        return new Message(
                new MessageId(MESSAGE_ID), ConversationId.random(), sender, new MessageText("hello"), AT);
    }

    private static Message messageAbout(String polishExerciseName) {
        return new Message(
                new MessageId(MESSAGE_ID),
                ConversationId.random(),
                CLIENT_ID,
                new MessageText("hello"),
                AT,
                new AboutSet(
                        new SetLogId(SET_LOG_ID),
                        new SessionLogId(SESSION_LOG_ID),
                        "Lower body",
                        LocalDate.of(2026, 11, 1),
                        new LocalizedText("Back squat", polishExerciseName),
                        0,
                        3,
                        9));
    }

    private MvcTestResult post(String body) {
        return mvc.post()
                .uri(MESSAGES)
                .with(CLIENT)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
                .exchange();
    }

    @Test
    void shouldListTheSignedInPersonsConversations() {
        when(messaging.listConversations(TRAINER_ID))
                .thenReturn(List.of(
                        new ConversationOverview(
                                CLIENT_ID,
                                "Ola Kowalska",
                                Goal.BUILD_MUSCLE,
                                new PlanWeek(3, 8),
                                2,
                                new LastMessage("Can we move it?", false, AT)),
                        new ConversationOverview(AccountId.random(), "Jan Nowak", null, null, 0, null)));

        var result = mvc.get().uri("/api/v1/conversations").with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.conversations[0].clientId").isEqualTo(CLIENT_ID.value().toString());
        json.extractingPath("$.conversations[0].name").isEqualTo("Ola Kowalska");
        json.extractingPath("$.conversations[0].goal").isEqualTo("BUILD_MUSCLE");
        json.extractingPath("$.conversations[0].currentWeek").isEqualTo(3);
        json.extractingPath("$.conversations[0].planWeeks").isEqualTo(8);
        json.extractingPath("$.conversations[0].unreadCount").isEqualTo(2);
        json.extractingPath("$.conversations[0].lastMessage.preview").isEqualTo("Can we move it?");
        json.extractingPath("$.conversations[0].lastMessage.sentByMe").isEqualTo(false);
        json.extractingPath("$.conversations[0].lastMessage.sentAt").isEqualTo("2026-11-02T09:12:00Z");
        json.doesNotHavePath("$.conversations[1].goal");
        json.doesNotHavePath("$.conversations[1].currentWeek");
        json.doesNotHavePath("$.conversations[1].lastMessage");
    }

    @Test
    void shouldAnswerTheUnreadCount() {
        when(messaging.unreadCount(CLIENT_ID)).thenReturn(3);

        var result = mvc.get().uri("/api/v1/conversations/unread-count").with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.count").isEqualTo(3);
    }

    @Test
    void shouldAnswerAPageFromTheReadersSide_withoutSenderOrEmail() throws Exception {
        when(messaging.messages(TRAINER_ID, CLIENT_ID, null))
                .thenReturn(new MessagePage(List.of(message(CLIENT_ID), message(TRAINER_ID)), true));

        var result = mvc.get().uri(MESSAGES).with(TRAINER).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        var json = assertThat(result).bodyJson();
        json.extractingPath("$.messages[0].id").isEqualTo(MESSAGE_ID.toString());
        json.extractingPath("$.messages[0].text").isEqualTo("hello");
        json.extractingPath("$.messages[0].mine").isEqualTo(false);
        json.extractingPath("$.messages[0].sentAt").isEqualTo("2026-11-02T09:12:00Z");
        json.extractingPath("$.messages[1].mine").isEqualTo(true);
        json.extractingPath("$.hasMore").isEqualTo(true);
        json.doesNotHavePath("$.messages[0].sender");
        json.doesNotHavePath("$.messages[0].senderId");
        assertThat(result.getResponse().getContentAsString()).doesNotContain(CLIENT_ID.value().toString(), "@");
    }

    @Test
    void shouldPassBeforeToTheUseCase() {
        when(messaging.messages(CLIENT_ID, CLIENT_ID, new MessageId(MESSAGE_ID))).thenReturn(MessagePage.empty());

        var result = mvc.get().uri(MESSAGES + "?before=" + MESSAGE_ID).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.OK);
        assertThat(result).bodyJson().extractingPath("$.messages").asArray().isEmpty();
    }

    @Test
    void shouldAnswerCreatedWithTheStoredMessage_whenSent() {
        when(messaging.send(new SendMessageCommand(CLIENT_ID, CLIENT_ID, new MessageId(MESSAGE_ID), "hello")))
                .thenReturn(message(CLIENT_ID));

        var result = post(BODY);

        assertThat(result).hasStatus(HttpStatus.CREATED);
        assertThat(result).bodyJson().extractingPath("$.id").isEqualTo(MESSAGE_ID.toString());
        assertThat(result).bodyJson().extractingPath("$.mine").isEqualTo(true);
    }

    @Test
    void shouldAnswerTheSetAMessageIsAbout_inAPageAndWhenSent() {
        when(messaging.messages(TRAINER_ID, CLIENT_ID, null))
                .thenReturn(new MessagePage(List.of(messageAbout("Przysiad"), message(TRAINER_ID)), false));
        when(messaging.send(new SendMessageCommand(
                        CLIENT_ID, CLIENT_ID, new MessageId(MESSAGE_ID), "hello", new SetLogId(SET_LOG_ID))))
                .thenReturn(messageAbout("Przysiad"));

        var page = mvc.get().uri(MESSAGES).with(TRAINER).exchange();
        var sent = post("{\"id\":\"" + MESSAGE_ID + "\",\"text\":\"hello\",\"aboutSetLogId\":\"" + SET_LOG_ID + "\"}");

        var about = assertThat(page).bodyJson();
        about.extractingPath("$.messages[0].about.setLogId").isEqualTo(SET_LOG_ID.toString());
        about.extractingPath("$.messages[0].about.sessionLogId").isEqualTo(SESSION_LOG_ID.toString());
        about.extractingPath("$.messages[0].about.sessionName").isEqualTo("Lower body");
        about.extractingPath("$.messages[0].about.sessionDate").isEqualTo("2026-11-01");
        about.extractingPath("$.messages[0].about.exerciseName").isEqualTo("Back squat");
        about.extractingPath("$.messages[0].about.entryPosition").isEqualTo(0);
        about.extractingPath("$.messages[0].about.setNumber").isEqualTo(3);
        about.extractingPath("$.messages[0].about.rpe").isEqualTo(9);
        about.doesNotHavePath("$.messages[1].about");
        assertThat(sent).hasStatus(HttpStatus.CREATED);
        assertThat(sent).bodyJson().extractingPath("$.about.setLogId").isEqualTo(SET_LOG_ID.toString());
    }

    @Test
    void shouldNameTheExerciseInPolish_whenTheRequestIsInPolishAndTheExerciseHasAPolishName() {
        when(messaging.messages(TRAINER_ID, CLIENT_ID, null))
                .thenReturn(new MessagePage(List.of(messageAbout("Przysiad"), messageAbout(null)), false));

        var result = mvc.get()
                .uri(MESSAGES)
                .with(TRAINER)
                .header(HttpHeaders.ACCEPT_LANGUAGE, "pl")
                .exchange();

        assertThat(result).bodyJson().extractingPath("$.messages[0].about.exerciseName").isEqualTo("Przysiad");
        assertThat(result).bodyJson().extractingPath("$.messages[1].about.exerciseName").isEqualTo("Back squat");
    }

    // one answer for another client's set, an unknown one and a trainer's attempt: the use case decides, the code is one
    @Test
    void shouldAnswerNotFoundWithTheCode_whenTheSetIsNotTheSenders() {
        when(messaging.send(any())).thenThrow(new AboutSetNotFoundException());

        var result = post("{\"id\":\"" + MESSAGE_ID + "\",\"text\":\"hello\",\"aboutSetLogId\":\"" + SET_LOG_ID + "\"}");

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("message.about-not-found");
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
    }

    @Test
    void shouldAnswerBadRequest_whenTheSetLogIdIsNoId() {
        var result = post("{\"id\":\"" + MESSAGE_ID + "\",\"text\":\"hello\",\"aboutSetLogId\":\"not-an-id\"}");

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(messaging);
    }

    @Test
    void shouldAnswerNoContent_whenMarkedRead() {
        var result = mvc.put().uri(READ).with(TRAINER).with(csrf()).exchange();

        assertThat(result).hasStatus(HttpStatus.NO_CONTENT);
        verify(messaging).markRead(TRAINER_ID, CLIENT_ID);
    }

    @Test
    void shouldAnswerBadRequestWithTheCode_whenTheMessageIsEmpty() {
        when(messaging.send(any())).thenThrow(new MessageEmptyException());

        var result = post(BODY);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("message.empty");
        assertThat(result).bodyJson().doesNotHavePath("$.detail");
    }

    @Test
    void shouldAnswerBadRequestWithTheCode_whenTheMessageIsTooLong() {
        when(messaging.send(any())).thenThrow(new MessageTooLongException());

        var result = post(BODY);

        assertThat(result).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("message.too-long");
    }

    @Test
    void shouldAnswerValidationFailed_whenIdOrTextIsMissing() {
        var noId = post("{\"text\":\"hello\"}");
        var noText = post("{\"id\":\"" + MESSAGE_ID + "\"}");

        assertThat(noId).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noId).bodyJson().extractingPath("$.code").isEqualTo("validation.failed");
        assertThat(noId).bodyJson().extractingPath("$.errors[0].field").isEqualTo("id");
        assertThat(noText).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(noText).bodyJson().extractingPath("$.errors[0].field").isEqualTo("text");
        verifyNoInteractions(messaging);
    }

    @Test
    void shouldAnswerRequestInvalid_whenAnIdIsNoId() {
        var badClient = mvc.get().uri("/api/v1/conversations/not-an-id/messages").with(CLIENT).exchange();
        var badBefore = mvc.get().uri(MESSAGES + "?before=not-an-id").with(CLIENT).exchange();

        assertThat(badClient).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badClient).bodyJson().extractingPath("$.code").isEqualTo("request.invalid");
        assertThat(badBefore).hasStatus(HttpStatus.BAD_REQUEST);
        assertThat(badBefore).bodyJson().extractingPath("$.code").isEqualTo("request.invalid");
        verifyNoInteractions(messaging);
    }

    @Test
    void shouldAnswerNotFoundWithTheCode_whenTheConversationIsNotThePersons() {
        when(messaging.messages(TRAINER_ID, CLIENT_ID, null)).thenThrow(new ConversationNotFoundException());
        when(messaging.send(any())).thenThrow(new ConversationNotFoundException());
        doThrow(new ConversationNotFoundException())
                .when(messaging)
                .markRead(TRAINER_ID, CLIENT_ID);

        var read = mvc.get().uri(MESSAGES).with(TRAINER).exchange();
        var send = post(BODY);
        var mark = mvc.put().uri(READ).with(TRAINER).with(csrf()).exchange();

        assertThat(read).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(read).bodyJson().extractingPath("$.code").isEqualTo("conversation.not-found");
        assertThat(send).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(send).bodyJson().extractingPath("$.code").isEqualTo("conversation.not-found");
        assertThat(mark).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(mark).bodyJson().extractingPath("$.code").isEqualTo("conversation.not-found");
    }

    @Test
    void shouldAnswerNotFoundWithTheCode_whenBeforeIsNotInTheConversation() {
        when(messaging.messages(CLIENT_ID, CLIENT_ID, new MessageId(MESSAGE_ID)))
                .thenThrow(new MessageNotFoundException());

        var result = mvc.get().uri(MESSAGES + "?before=" + MESSAGE_ID).with(CLIENT).exchange();

        assertThat(result).hasStatus(HttpStatus.NOT_FOUND);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("message.not-found");
    }

    @Test
    void shouldAnswerConflictWithTheCode_whenTheIdIsInUse() {
        when(messaging.send(any())).thenThrow(new MessageIdInUseException());

        var result = post(BODY);

        assertThat(result).hasStatus(HttpStatus.CONFLICT);
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("message.id-in-use");
    }

    @Test
    void shouldAskToSignIn_whenNobodyIsSignedIn() {
        assertThat(mvc.get().uri("/api/v1/conversations").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri("/api/v1/conversations/unread-count").exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.get().uri(MESSAGES).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.post()
                        .uri(MESSAGES)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(BODY)
                        .exchange())
                .hasStatus(HttpStatus.UNAUTHORIZED);
        assertThat(mvc.put().uri(READ).with(csrf()).exchange()).hasStatus(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(messaging);
    }

    @Test
    void shouldRefuseAWrite_whenTheCsrfTokenIsMissing() {
        var send = mvc.post()
                .uri(MESSAGES)
                .with(CLIENT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(BODY)
                .exchange();
        var mark = mvc.put().uri(READ).with(CLIENT).exchange();

        assertThat(send).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(mark).hasStatus(HttpStatus.FORBIDDEN);
        verifyNoInteractions(messaging);
    }

    @Test
    void shouldNotShowTheExceptionMessage_whenAnythingFails() throws Exception {
        when(messaging.send(any())).thenThrow(new IllegalStateException("secret text from the message"));

        var result = post(BODY);

        assertThat(result).hasStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(result.getResponse().getContentAsString()).doesNotContain("secret", "IllegalState");
        assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("internal.error");
    }
}
