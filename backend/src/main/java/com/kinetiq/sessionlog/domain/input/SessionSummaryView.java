package com.kinetiq.sessionlog.domain.input;

import com.kinetiq.sessionlog.domain.model.SessionLog;
import com.kinetiq.sessionlog.domain.model.SessionSummary;

/** A finished session log with its summary, which is worked out from the log as it is now. */
public record SessionSummaryView(SessionLog log, SessionSummary summary) {}
