package com.kinetiq.account.infrastructure.input;

import com.kinetiq.account.domain.model.Account;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// strings rather than enums, so a wrong value is reported on its field instead of as an unreadable body
record UpdateAccountRequest(
        // may be left out; when sent, not blank and without control characters, as at sign-up
        @Size(max = Account.NAME_MAX_LENGTH) @Pattern(regexp = "^\\P{Cc}*[^\\p{Cc}\\s]\\P{Cc}*$") String name,
        @Pattern(regexp = "KG|LB") String units,
        @Pattern(regexp = "EN|PL") String language,
        @Pattern(regexp = "LIGHT|DARK|SYSTEM") String theme) {}
