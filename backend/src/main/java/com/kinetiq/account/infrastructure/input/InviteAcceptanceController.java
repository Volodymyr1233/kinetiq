package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.exception.InviteLinkInvalidException;
import com.kinetiq.account.domain.input.InviteDetails;
import com.kinetiq.account.domain.input.JoinCommand;
import com.kinetiq.account.domain.input.SignUpUseCase;
import com.kinetiq.account.domain.model.Account;
import com.kinetiq.account.domain.model.RawPassword;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
class InviteAcceptanceController {

    private static final String UNIQUE_EMAIL = "uq_accounts_email";

    private final SignUpUseCase signUp;
    private final AccountWebMapper mapper;
    private final SignIns signIns;

    /**
     * Answers the invited email and the trainer's name for a working invite link, and changes nothing. Every link that
     * does not work gets the same {@code invite.link-invalid}.
     */
    @PostMapping("/invite-checks")
    InviteCheckResponse checkInvite(@Valid @RequestBody InviteCheckRequest request) {
        InviteDetails details = signUp.checkInvite(request.token());
        return new InviteCheckResponse(details.email().value(), details.trainerName());
    }

    /**
     * Creates the client of the inviting trainer and signs them in on this browser, replacing any earlier sign-in
     * here. A link that does not work, also one that stopped working after it was checked, gets
     * {@code invite.link-invalid} and creates nothing.
     */
    @PostMapping("/invite-acceptances")
    @ResponseStatus(HttpStatus.CREATED)
    CurrentAccountResponse acceptInvite(
            @Valid @RequestBody AcceptInviteRequest body,
            Locale locale,
            HttpServletRequest request,
            HttpServletResponse response) {
        JoinCommand command = new JoinCommand(
                body.token(), body.name(), new RawPassword(body.password()), TimeZones.parse(body.timeZone()), locale);
        Account account;
        try {
            account = signUp.joinAsClient(command);
        } catch (OptimisticLockingFailureException e) {
            throw new InviteLinkInvalidException();
        } catch (DataIntegrityViolationException e) {
            // the loser of two simultaneous joins; any other violation is a bug and stays a 500
            if (!violates(e, UNIQUE_EMAIL)) {
                throw e;
            }
            throw new InviteLinkInvalidException();
        }
        signIns.start(account, request, response);
        return mapper.toResponse(account);
    }

    private static boolean violates(Throwable error, String constraint) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return constraint.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
