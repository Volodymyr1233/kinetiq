package com.kinetiq.common.domain.output;

import com.kinetiq.common.domain.model.Email;
import java.util.Locale;
import java.util.Map;

/** An email to send. The adapter renders the template in the given language. */
public record Mail(Email to, String template, Locale locale, Map<String, String> values) {

    public Mail {
        values = Map.copyOf(values);
    }
}
