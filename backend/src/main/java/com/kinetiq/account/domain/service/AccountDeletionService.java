package com.kinetiq.account.domain.service;

import com.kinetiq.account.domain.exception.AccountNotFoundException;
import com.kinetiq.account.domain.exception.PasswordIncorrectException;
import com.kinetiq.account.domain.exception.SignInLockedException;
import com.kinetiq.account.domain.exception.TrainerHasClientsException;
import com.kinetiq.account.domain.input.DeleteAccountUseCase;
import com.kinetiq.account.domain.input.DeletionCheck;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.AccountId;
import com.kinetiq.common.domain.model.Notice;
import com.kinetiq.common.domain.model.NoticeKind;
import com.kinetiq.common.domain.output.NoticeBoard;
import com.kinetiq.account.domain.model.FailedSignIns;
import com.kinetiq.account.domain.model.RawPassword;
import com.kinetiq.account.domain.model.Role;
import com.kinetiq.account.domain.output.AccountRepository;
import com.kinetiq.account.domain.output.AccountSessions;
import com.kinetiq.account.domain.output.FailedSignInsRepository;
import com.kinetiq.account.domain.output.InviteRepository;
import com.kinetiq.account.domain.output.PasswordHasher;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AccountDeletionService implements DeleteAccountUseCase {

    private final AccountRepository accounts;
    private final InviteRepository invites;
    private final PasswordHasher hasher;
    private final FailedSignInsRepository failedSignIns;
    private final AccountSessions sessions;
    private final NoticeBoard notices;
    private final Clock clock;

    @Override
    public DeletionCheck check(AccountId id) {
        Account account = find(id);
        if (account.getRole() == Role.TRAINER) {
            return new DeletionCheck(accounts.countClientsOf(id), null);
        }
        String trainerName = account.getTrainerId()
                .flatMap(accounts::findById)
                .map(Account::getName)
                .orElse(null);
        return new DeletionCheck(0, trainerName);
    }

    @Override
    public void delete(AccountId id, String password) {
        Account account = find(id);
        confirm(account, password);
        if (account.getRole() == Role.TRAINER) {
            int clients = accounts.countClientsOf(id);
            if (clients > 0) {
                throw new TrainerHasClientsException(clients);
            }
        }
        // an invite names the client by email, not by key, so the database cannot erase it with the account
        account.getTrainerId().ifPresent(trainer -> invites.deleteByTrainerAndEmail(trainer, account.getEmail()));
        accounts.delete(id);
        // the trainer is told that a client left, and nothing of who: no name, no email, no id of the client
        if (account.getRole() == Role.CLIENT) {
            account.getTrainerId()
                    .ifPresent(trainer -> notices.raise(new Notice(trainer.value(), null, NoticeKind.CLIENT_DELETED)));
        }
        // last, and in its own transaction, so a refused deletion signs nobody out
        sessions.endAll(id);
    }

    // the same lock as sign-in, so a stolen sign-in cannot guess the password here faster than there
    private void confirm(Account account, String password) {
        Instant now = clock.instant();
        Optional<FailedSignIns> failed = failedSignIns.find(account.getEmail());
        Optional<Duration> timeLeft = failed.flatMap(entry -> entry.timeLeft(now));
        if (timeLeft.isPresent()) {
            throw new SignInLockedException(timeLeft.get());
        }
        // no account can have such a password, so there is nothing to hash
        boolean matches =
                password.length() <= RawPassword.MAX_LENGTH && hasher.matches(password, account.getPasswordHash());
        if (!matches) {
            FailedSignIns entry = failed.orElseGet(() -> new FailedSignIns(account.getEmail()));
            entry.recordFailure(now);
            failedSignIns.save(entry);
            throw new PasswordIncorrectException();
        }
        failedSignIns.delete(account.getEmail());
    }

    private Account find(AccountId id) {
        return accounts.findById(id).orElseThrow(AccountNotFoundException::new);
    }
}
