package com.kinetiq.plan.infrastructure.output;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kinetiq.plan.domain.exception.PlanGenerationFailedException;
import com.kinetiq.plan.domain.model.GeneratedPlan;
import com.kinetiq.plan.domain.model.PlanPromptMessages;
import com.kinetiq.plan.domain.output.PlanGenerator;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the model behind OpenRouter for a plan. It keeps out of the log everything that could hold client text: the
 * prompt, the answer and the message of an exception, which can quote either.
 */
@Slf4j
@Component
@ConditionalOnExpression("!'${kinetiq.plan-drafting.api-key:}'.isBlank()")
@RequiredArgsConstructor
class OpenRouterPlanGenerator implements PlanGenerator {

    private static final Pattern CODE_FENCE = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL);

    private static final String CUT_OFF = "length";

    // a value of the wrong kind is a bad answer: rounding 7.5 to an RPE of 7 would hide it
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    private final ChatModel chatModel;

    @Override
    public GeneratedPlan generate(PlanPromptMessages messages) {
        long started = System.nanoTime();
        try {
            // no options here: options on a prompt replace the model's defaults instead of adding to them
            ChatResponse response = chatModel.call(new Prompt(toMessages(messages)));
            log.info("Plan attempt answered, model={}, duration={}", response.getMetadata().getModel(), since(started));
            return parse(response);
        } catch (PlanGenerationFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn(
                    "Plan attempt failed, cause={}, duration={}",
                    exception.getClass().getSimpleName(),
                    since(started));
            throw new PlanGenerationFailedException("the service call failed");
        }
    }

    private static List<Message> toMessages(PlanPromptMessages messages) {
        List<Message> all = new ArrayList<>(List.of(new SystemMessage(messages.system()), new UserMessage(messages.user())));
        if (!messages.rejections().isEmpty()) {
            all.add(new UserMessage(rejectionMessage(messages.rejections())));
        }
        return all;
    }

    private static String rejectionMessage(List<String> rejections) {
        return rejections.stream()
                .map(sentence -> "- " + sentence)
                .collect(Collectors.joining(
                        "\n",
                        "Your previous answer was rejected for these reasons:\n",
                        "\nAnswer again with a complete, corrected plan."));
    }

    private static GeneratedPlan parse(ChatResponse response) {
        Generation generation = response.getResult();
        if (generation == null) {
            throw new PlanGenerationFailedException("the answer was empty");
        }
        if (CUT_OFF.equalsIgnoreCase(generation.getMetadata().getFinishReason())) {
            throw new PlanGenerationFailedException("the answer was cut off");
        }
        String content = generation.getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new PlanGenerationFailedException("the answer was empty");
        }
        try {
            return JSON.readValue(withoutCodeFence(content), Answer.class).toDomain();
        } catch (JacksonException | NullPointerException exception) {
            throw new PlanGenerationFailedException("the answer was not of the expected shape");
        }
    }

    private static String withoutCodeFence(String content) {
        String trimmed = content.strip();
        Matcher fenced = CODE_FENCE.matcher(trimmed);
        return fenced.matches() ? fenced.group(1) : trimmed;
    }

    private static Duration since(long started) {
        return Duration.ofNanos(System.nanoTime() - started);
    }

    // a missing target stays null for the checks to name; only a missing structure is a shape that cannot be read
    private record Answer(List<AnswerSession> sessions) {

        GeneratedPlan toDomain() {
            return new GeneratedPlan(Objects.requireNonNull(sessions).stream()
                    .map(AnswerSession::toDomain)
                    .toList());
        }
    }

    private record AnswerSession(String name, String day, List<AnswerEntry> entries) {

        GeneratedPlan.GeneratedSession toDomain() {
            return new GeneratedPlan.GeneratedSession(
                    name, day, Objects.requireNonNull(entries).stream().map(AnswerEntry::toDomain).toList());
        }
    }

    private record AnswerEntry(
            @JsonProperty("exercise") String exerciseKey,
            Integer sets,
            Integer amount,
            BigDecimal loadKg,
            Integer rpe,
            Integer restSeconds) {

        GeneratedPlan.GeneratedEntry toDomain() {
            return new GeneratedPlan.GeneratedEntry(exerciseKey, sets, amount, loadKg, rpe, restSeconds);
        }
    }
}
