package com.kinetiq.account.infrastructure.input;

import jakarta.validation.constraints.Pattern;

// strings rather than enums, so a wrong value is reported on its field instead of as an unreadable body
record UpdateAccountRequest(
        @Pattern(regexp = "KG|LB") String units,
        @Pattern(regexp = "EN|PL") String language,
        @Pattern(regexp = "LIGHT|DARK|SYSTEM") String theme) {}
