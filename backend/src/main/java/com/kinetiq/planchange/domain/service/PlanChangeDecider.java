package com.kinetiq.planchange.domain.service;

import com.kinetiq.account.domain.model.Language;
import com.kinetiq.messaging.domain.input.ExchangeMessagesUseCase;
import com.kinetiq.messaging.domain.input.SendMessageCommand;
import com.kinetiq.messaging.domain.model.MessageId;
import com.kinetiq.planchange.domain.exception.PlanChangeNotFoundException;
import com.kinetiq.planchange.domain.input.KeepCurrentPlanCommand;
import com.kinetiq.planchange.domain.model.Decision;
import com.kinetiq.planchange.domain.model.PlanChange;
import com.kinetiq.planchange.domain.model.PlanChangeId;
import com.kinetiq.planchange.domain.model.Side;
import com.kinetiq.planchange.domain.output.PlanChangeRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;

/**
 * The steps both people share when they decide: read the plan change under a lock so decisions on it follow one
 * another, apply the transition of the aggregate, store the result. Who may decide is checked before.
 */
@RequiredArgsConstructor
class PlanChangeDecider {

    private final PlanChangeRepository planChanges;
    private final PlanChangeTexts texts;
    private final ExchangeMessagesUseCase messages;
    private final Clock clock;

    /** {@code zone} is the client's: the effective week begins in their time zone. */
    PlanChange accept(PlanChangeId id, Side side, ZoneId zone) {
        Instant now = clock.instant();
        return decide(id, change -> change.accept(side, now, LocalDate.ofInstant(now, zone)));
    }

    PlanChange undo(PlanChangeId id, Side side, ZoneId zone) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), zone);
        return decide(id, change -> change.undo(side, today));
    }

    /**
     * Ends the plan change as declined and posts the reason as the decliner. The message goes first so that a failure
     * to send leaves the plan change open; both are one transaction, so a failure to store undoes the message.
     */
    PlanChange keepCurrent(KeepCurrentPlanCommand command, Side side, ZoneId zone) {
        Instant now = clock.instant();
        PlanChange locked = planChanges.findForUpdate(command.id()).orElseThrow(PlanChangeNotFoundException::new);
        PlanChange declined =
                locked.decline(side, command.reason(), command.note(), now, LocalDate.ofInstant(now, zone));
        Language language = texts.languageOf(command.person());
        Decision decision = declined.getDecision().orElseThrow();
        String text = DeclineMessageWording.of(
                language, texts.text(language, locked).title(), decision.reason(), decision.note());
        messages.send(new SendMessageCommand(command.person(), locked.getClient(), messageId(locked.getId()), text));
        return planChanges.update(declined);
    }

    // one id per plan change, so a repeated attempt can never post a second message
    private static MessageId messageId(PlanChangeId id) {
        return new MessageId(UUID.nameUUIDFromBytes(("plan-change-declined:" + id.value()).getBytes(StandardCharsets.UTF_8)));
    }

    private PlanChange decide(PlanChangeId id, Function<PlanChange, PlanChange> transition) {
        PlanChange locked = planChanges.findForUpdate(id).orElseThrow(PlanChangeNotFoundException::new);
        PlanChange changed = transition.apply(locked);
        return changed == locked ? locked : planChanges.update(changed);
    }
}
