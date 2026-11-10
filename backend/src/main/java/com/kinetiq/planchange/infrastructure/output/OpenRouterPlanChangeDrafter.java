package com.kinetiq.planchange.infrastructure.output;

import com.kinetiq.planchange.domain.exception.PlanChangeDraftingFailedException;
import com.kinetiq.planchange.domain.model.PlanChangePromptMessages;
import com.kinetiq.planchange.domain.model.ProposedContent;
import com.kinetiq.planchange.domain.output.PlanChangeDrafter;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

/**
 * Asks the model behind OpenRouter for plan change content. It logs neither the prompt nor the answer nor the message
 * of an exception, which can quote either; only the exception class.
 */
@Slf4j
@RequiredArgsConstructor
public class OpenRouterPlanChangeDrafter implements PlanChangeDrafter {

    /** The JSON schema the answer must match. */
    public static final String ANSWER_SCHEMA =
            """
            {
              "type": "object",
              "additionalProperties": false,
              "required": ["changes", "removeSession", "moves"],
              "properties": {
                "changes": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["entry", "sets", "amount", "loadKg"],
                    "properties": {
                      "entry": { "type": "string" },
                      "sets": { "type": ["integer", "null"] },
                      "amount": { "type": ["integer", "null"] },
                      "loadKg": { "type": ["number", "null"] }
                    }
                  }
                },
                "removeSession": { "type": ["string", "null"] },
                "moves": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "additionalProperties": false,
                    "required": ["entry", "toSession"],
                    "properties": {
                      "entry": { "type": "string" },
                      "toSession": { "type": "string" }
                    }
                  }
                }
              }
            }
            """;

    private static final Pattern CODE_FENCE = Pattern.compile("^```(?:json)?\\s*(.*?)\\s*```$", Pattern.DOTALL);
    private static final String CUT_OFF = "length";

    // a value of the wrong kind is a bad answer: 7.5 repetitions must not become 7
    private static final JsonMapper JSON = JsonMapper.builder()
            .disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT)
            .build();

    private final ChatModel chatModel;

    @Override
    public ProposedContent draft(PlanChangePromptMessages messages) {
        long started = System.nanoTime();
        try {
            // no options here: options on a prompt replace the model's defaults instead of adding to them
            ChatResponse response = chatModel.call(new Prompt(toMessages(messages)));
            log.info(
                    "Plan change attempt answered, model={}, duration={}",
                    response.getMetadata().getModel(),
                    Duration.ofNanos(System.nanoTime() - started));
            return parse(response);
        } catch (PlanChangeDraftingFailedException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.warn("Plan change attempt failed, cause={}", exception.getClass().getSimpleName());
            throw new PlanChangeDraftingFailedException("the service call failed");
        }
    }

    private static List<Message> toMessages(PlanChangePromptMessages messages) {
        List<Message> all =
                new ArrayList<>(List.of(new SystemMessage(messages.system()), new UserMessage(messages.user())));
        if (!messages.rejections().isEmpty()) {
            all.add(new UserMessage(messages.rejections().stream()
                    .map(sentence -> "- " + sentence)
                    .collect(Collectors.joining(
                            "\n",
                            "Your previous answer was rejected for these reasons:\n",
                            "\nAnswer again with corrected content."))));
        }
        return all;
    }

    private static ProposedContent parse(ChatResponse response) {
        Generation generation = response.getResult();
        if (generation == null) {
            throw new PlanChangeDraftingFailedException("the answer was empty");
        }
        if (CUT_OFF.equalsIgnoreCase(generation.getMetadata().getFinishReason())) {
            throw new PlanChangeDraftingFailedException("the answer was cut off");
        }
        String content = generation.getOutput().getText();
        if (content == null || content.isBlank()) {
            throw new PlanChangeDraftingFailedException("the answer was empty");
        }
        try {
            return JSON.readValue(withoutCodeFence(content), Answer.class).toDomain();
        } catch (JacksonException | NullPointerException exception) {
            throw new PlanChangeDraftingFailedException("the answer was not of the expected shape");
        }
    }

    private static String withoutCodeFence(String content) {
        String trimmed = content.strip();
        Matcher fenced = CODE_FENCE.matcher(trimmed);
        return fenced.matches() ? fenced.group(1) : trimmed;
    }

    private record Answer(List<AnswerChange> changes, String removeSession, List<AnswerMove> moves) {

        ProposedContent toDomain() {
            return new ProposedContent(
                    Objects.requireNonNull(changes).stream()
                            .map(change -> new ProposedContent.Change(
                                    change.entry(), change.sets(), change.amount(), change.loadKg()))
                            .toList(),
                    Optional.ofNullable(removeSession),
                    Objects.requireNonNull(moves).stream()
                            .map(move -> new ProposedContent.Move(move.entry(), move.toSession()))
                            .toList());
        }
    }

    private record AnswerChange(String entry, Integer sets, Integer amount, BigDecimal loadKg) {}

    private record AnswerMove(String entry, String toSession) {}
}
