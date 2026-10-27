package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.ConfirmationLinkInvalidException;
import com.kinetiq.account.domain.exception.InviteLinkInvalidException;
import com.kinetiq.account.domain.input.InviteDetails;
import com.kinetiq.account.domain.input.JoinCommand;
import com.kinetiq.account.domain.input.SignUpTrainerCommand;
import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountSettings;
import com.kinetiq.account.domain.model.Invite;
import com.kinetiq.account.domain.model.Language;
import com.kinetiq.account.domain.model.SecretToken;
import com.kinetiq.account.domain.model.Theme;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.InviteRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import com.kinetiq.common.domain.model.Email;
import com.kinetiq.common.domain.output.Mail;
import com.kinetiq.common.domain.output.MailSender;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class SignUpService implements SignUpUseCase {

    private static final String CONFIRM_EMAIL = "confirm-email";
    private static final String ACCOUNT_EXISTS = "account-exists";

    private final AccountRepository accounts;
    private final InviteRepository invites;
    private final PasswordHasher hasher;
    private final MailSender mailSender;
    private final Clock clock;
    private final URI appUrl;

    @Override
    public void signUpTrainer(SignUpTrainerCommand command) {
        // hashed on every branch, so the response time does not reveal whether the email has an account
        // ponytail: hashing inside the use-case transaction holds a database connection meanwhile; hash before
        // the transaction if sign-up load ever matters
        String passwordHash = hasher.hash(command.password());
        Instant now = clock.instant();
        accounts.findByEmail(command.email())
                .ifPresentOrElse(
                        existing -> remind(existing, command.locale(), now),
                        () -> sendConfirmation(
                                Account.signUpTrainer(
                                        command.name(),
                                        command.email(),
                                        passwordHash,
                                        command.timeZone(),
                                        Language.from(command.locale())),
                                command.locale(),
                                now));
    }

    @Override
    public void confirmEmail(String token) {
        Account account = accounts.findByConfirmationTokenHash(new SecretToken(token).hash())
                .orElseThrow(ConfirmationLinkInvalidException::new);
        account.confirmEmail(clock.instant());
        accounts.save(account);
    }

    @Override
    public void sendConfirmationLink(Email email, Locale locale) {
        Instant now = clock.instant();
        accounts.findByEmail(email)
                .filter(account -> !account.isEmailConfirmed() && account.canReceiveMail(now))
                .ifPresent(account -> sendConfirmation(account, locale, now));
    }

    @Override
    public InviteDetails checkInvite(String token) {
        Invite invite = workingInvite(new SecretToken(token), clock.instant());
        return new InviteDetails(invite.getEmail(), trainerOf(invite).getName());
    }

    @Override
    public Account joinAsClient(JoinCommand command) {
        SecretToken token = new SecretToken(command.token());
        Instant now = clock.instant();
        Invite invite = workingInvite(token, now);
        // hashed only for a working link, so a wrong link costs nothing
        String passwordHash = hasher.hash(command.password());
        Account trainer = trainerOf(invite);
        Account client = Account.joinAsClient(
                command.name(),
                invite.getEmail(),
                passwordHash,
                command.timeZone(),
                new AccountSettings(
                        trainer.getSettings().units(), Language.from(command.locale()), Theme.SYSTEM),
                trainer.getId(),
                now,
                invite.provesEmail(token));
        invite.use(now);
        accounts.save(client);
        invites.save(invite);
        return client;
    }

    private Invite workingInvite(SecretToken token, Instant now) {
        return invites.findByTokenHash(token.hash())
                .filter(invite -> invite.isOpen(now) && !accounts.existsByEmail(invite.getEmail()))
                .orElseThrow(InviteLinkInvalidException::new);
    }

    private Account trainerOf(Invite invite) {
        return accounts.findById(invite.getTrainerId()).orElseThrow();
    }

    // the account itself never changes here; only when it last got an email
    private void remind(Account account, Locale locale, Instant now) {
        if (!account.canReceiveMail(now)) {
            return;
        }
        if (!account.isEmailConfirmed()) {
            sendConfirmation(account, locale, now);
            return;
        }
        account.recordMailSent(now);
        accounts.save(account);
        mailSender.send(new Mail(account.getEmail(), ACCOUNT_EXISTS, locale, Map.of("link", appUrl + "/sign-in")));
    }

    private void sendConfirmation(Account account, Locale locale, Instant now) {
        SecretToken token = SecretToken.generate();
        account.issueConfirmation(token, now);
        accounts.save(account);
        // a fragment never reaches a server, so the token stays out of access logs and Referer headers
        String link = appUrl + "/confirm-email#token=" + token.value();
        mailSender.send(new Mail(account.getEmail(), CONFIRM_EMAIL, locale, Map.of("link", link)));
    }
}
