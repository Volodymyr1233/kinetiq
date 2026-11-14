package com.kinetiq.progress.domain.input;

import com.kinetiq.progress.domain.model.Pr;
import java.util.List;

/** What a client sees besides a session's summary: the PRs of that session and their week streak. */
public record Rewards(List<Pr> prs, int weeksInARow) {

    public Rewards {
        prs = List.copyOf(prs);
    }
}
