package com.kinetiq.plan.infrastructure.output;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import com.kinetiq.plan.domain.exception.PlanGenerationFailedException;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.PlanPromptMessages;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

class OpenRouterPlanGeneratorTest {

    private static final String VALID_ANSWER =
            """
            {"sessions":[{"name":"Lower body A","day":"WEDNESDAY","entries":[
              {"exercise":"E1","sets":3,"amount":10,"loadKg":22.5,"rpe":7,"restSeconds":90},
              {"exercise":"E2","sets":2,"amount":30,"loadKg":null,"rpe":6,"restSeconds":60}
            ]}]}
            """;

    private static final PlanPromptMessages FIRST_ATTEMPT = new PlanPromptMessages("the system", "the user", List.of());

    /** Answers with whatever the test gives, and remembers the prompt. */
    private static final class FakeChatModel implements ChatModel {

        private final Function<Prompt, ChatResponse> answer;
        private Prompt received;

        private FakeChatModel(Function<Prompt, ChatResponse> answer) {
            this.answer = answer;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            received = prompt;
            return answer.apply(prompt);
        }
    }

    private static ChatResponse answering(String content, String finishReason) {
        return new ChatResponse(List.of(new Generation(
                new AssistantMessage(content),
                ChatGenerationMetadata.builder().finishReason(finishReason).build())));
    }

    private static OpenRouterPlanGenerator generator(String content) {
        return new OpenRouterPlanGenerator(new FakeChatModel(prompt -> answering(content, "STOP")));
    }

    @Test
    void shouldMapAnswerToDomainObjects_whenAnswerIsValid() {
        GeneratedPlan plan = generator(VALID_ANSWER).generate(FIRST_ATTEMPT);

        assertThat(plan.sessions()).hasSize(1);
        assertThat(plan.sessions().getFirst().name()).isEqualTo("Lower body A");
        assertThat(plan.sessions().getFirst().day()).isEqualTo("WEDNESDAY");
        assertThat(plan.sessions().getFirst().entries())
                .containsExactly(
                        new GeneratedPlan.GeneratedEntry("E1", 3, 10, new BigDecimal("22.5"), 7, 90),
                        new GeneratedPlan.GeneratedEntry("E2", 2, 30, null, 6, 60));
    }

    @Test
    void shouldAcceptAnswer_whenItIsInCodeFence() {
        GeneratedPlan plan = generator("```json\n" + VALID_ANSWER + "```\n").generate(FIRST_ATTEMPT);

        assertThat(plan.sessions()).hasSize(1);
    }

    @Test
    void shouldKeepMissingValueVisible_whenTargetIsAbsent() {
        GeneratedPlan plan = generator(
                        """
                        {"sessions":[{"name":"A","entries":[{"exercise":"E1","amount":10,"rpe":7,"restSeconds":90}]}]}
                        """)
                .generate(FIRST_ATTEMPT);

        assertThat(plan.sessions().getFirst().entries().getFirst().sets()).isNull();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "",
                "   ",
                "I am sorry, I cannot draft a plan.",
                "{\"plan\":[]}",
                "{\"sessions\":\"none\"}",
                "{\"sessions\":[\",{\"]}",
                "{\"sessions\":[null]}",
                "{\"sessions\":[{\"name\":\"A\"}]}",
                "{\"sessions\":[{\"name\":\"A\",\"entries\":[null]}]}",
                "{\"sessions\":[{\"name\":\"A\",\"entries\":[{\"exercise\":\"E1\",\"rpe\":7.5}]}]}"
            })
    void shouldFailAttempt_whenAnswerHasNoUsableShape(String content) {
        assertThatThrownBy(() -> generator(content).generate(FIRST_ATTEMPT))
                .isInstanceOf(PlanGenerationFailedException.class);
    }

    @Test
    void shouldFailAttempt_whenAnswerWasCutOff() {
        var model = new FakeChatModel(prompt -> answering(VALID_ANSWER, "LENGTH"));

        assertThatThrownBy(() -> new OpenRouterPlanGenerator(model).generate(FIRST_ATTEMPT))
                .isInstanceOf(PlanGenerationFailedException.class);
    }

    @Test
    void shouldFailAttempt_whenThereIsNoGeneration() {
        var model = new FakeChatModel(prompt -> new ChatResponse(List.of()));

        assertThatThrownBy(() -> new OpenRouterPlanGenerator(model).generate(FIRST_ATTEMPT))
                .isInstanceOf(PlanGenerationFailedException.class);
    }

    @Test
    void shouldFailAttempt_whenClientThrows() {
        var model = new FakeChatModel(prompt -> {
            throw new IllegalStateException("too many requests");
        });

        assertThatThrownBy(() -> new OpenRouterPlanGenerator(model).generate(FIRST_ATTEMPT))
                .isInstanceOf(PlanGenerationFailedException.class);
    }

    @Test
    void shouldSendSystemAndUserOnly_whenFirstAttempt() {
        var model = new FakeChatModel(prompt -> answering(VALID_ANSWER, "STOP"));

        new OpenRouterPlanGenerator(model).generate(FIRST_ATTEMPT);

        assertThat(model.received.getInstructions())
                .extracting(Message::getMessageType, Message::getText)
                .containsExactly(
                        tuple(MessageType.SYSTEM, "the system"),
                        tuple(MessageType.USER, "the user"));
    }

    @Test
    void shouldSendRejectionsAsFinalUserMessage_whenSecondAttempt() {
        var model = new FakeChatModel(prompt -> answering(VALID_ANSWER, "STOP"));

        new OpenRouterPlanGenerator(model)
                .generate(new PlanPromptMessages("the system", "the user", List.of("Too few sessions.", "Bad rest.")));

        List<Message> sent = model.received.getInstructions();
        assertThat(sent).hasSize(3);
        assertThat(sent.getLast().getMessageType()).isEqualTo(MessageType.USER);
        assertThat(sent.getLast().getText())
                .isEqualTo(
                        """
                        Your previous answer was rejected for these reasons:
                        - Too few sessions.
                        - Bad rest.
                        Answer again with a complete, corrected plan.""");
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldKeepPromptAnswerAndServiceTextOutOfLog_whenAttemptsEndAnyWay(CapturedOutput log) {
        PlanPromptMessages sentinel = new PlanPromptMessages("SENTINEL-SYSTEM", "SENTINEL-USER", List.of("SENTINEL-WHY"));

        new OpenRouterPlanGenerator(new FakeChatModel(prompt ->
                        answering(VALID_ANSWER.replace("Lower body A", "SENTINEL-ANSWER"), "STOP")))
                .generate(sentinel);
        for (var model : List.of(
                new FakeChatModel(prompt -> answering("SENTINEL-ANSWER is not JSON", "STOP")),
                new FakeChatModel(prompt -> {
                    throw new IllegalStateException("SENTINEL-MESSAGE from the service");
                }))) {
            assertThatThrownBy(() -> new OpenRouterPlanGenerator(model).generate(sentinel))
                    .isInstanceOf(PlanGenerationFailedException.class)
                    .satisfies(thrown -> assertThat(thrown.getMessage()).doesNotContain("SENTINEL"));
        }

        assertThat(log.getAll()).contains("cause=IllegalStateException").doesNotContain("SENTINEL");
    }
}
