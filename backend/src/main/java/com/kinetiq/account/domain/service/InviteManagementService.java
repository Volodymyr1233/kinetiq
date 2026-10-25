package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.InviteAccountExistsException;
import com.kinetiq.account.domain.exception.InviteNotFoundException;
import com.kinetiq.account.domain.input.CreateInviteCommand;
import com.kinetiq.account.domain.input.CreatedInvite;
import com.kinetiq.account.domain.input.ManageInvitesUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.InviteId;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.SentInvite;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.InviteRepository;
import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class InviteManagementService implements ManageInvitesUseCase {

    private static final String INVITE = "invite";

    private final InviteRepository invites;
    private final AccountRepository accounts;
    private final MailSender mailSender;
    private final Clock clock;
    private final URI appUrl;

    @Override
    public CreatedInvite create(CreateInviteCommand command) {
        if (accounts.existsByEmail(command.email())) {
            throw new InviteAccountExistsException();
        }
        Instant now = clock.instant();
        // ponytail: of two identical requests at the same moment, the one-open-invite index fails the second with the
        // generic error; retry inside the use case if trainers ever hit it
        invites.findOpenByTrainerAndEmail(command.trainerId(), command.email()).ifPresent(earlier -> {
            earlier.supersede();
            invites.save(earlier);
        });
        SentInvite sent = Invite.send(command.trainerId(), command.email(), now);
        invites.save(sent.invite());
        Account trainer = accounts.findById(command.trainerId()).orElseThrow();
        mailSender.send(new Mail(
                command.email(),
                INVITE,
                command.locale(),
                Map.of(
                        "link", link(sent.emailToken()),
                        "trainerName", trainer.getName(),
                        "validDays", String.valueOf(Invite.VALIDITY.toDays()))));
        return new CreatedInvite(sent.invite(), link(sent.linkToken()));
    }

    @Override
    public List<Invite> listOpen(AccountId trainerId) {
        return invites.findOpenByTrainer(trainerId, clock.instant());
    }

    @Override
    public String replaceLink(AccountId trainerId, InviteId id) {
        Invite invite = own(trainerId, id);
        SecretToken token = invite.replaceLink(clock.instant());
        invites.save(invite);
        return link(token);
    }

    @Override
    public void revoke(AccountId trainerId, InviteId id) {
        Invite invite = own(trainerId, id);
        invite.revoke(clock.instant());
        invites.save(invite);
    }

    // another trainer's invite is answered like an unknown one
    private Invite own(AccountId trainerId, InviteId id) {
        return invites.findById(id)
                .filter(invite -> invite.getTrainerId().equals(trainerId))
                .orElseThrow(InviteNotFoundException::new);
    }

    // a fragment never reaches a server, so the secret stays out of access logs and Referer headers
    private String link(SecretToken token) {
        return appUrl + "/join#token=" + token.value();
    }
}
