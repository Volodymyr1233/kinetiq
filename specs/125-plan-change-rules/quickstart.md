# Quickstart: validating the plan change rules

Prerequisites: Docker running; run commands in `backend/`.

## Automated

```bash
./gradlew test --tests 'com.kinetiq.planchange.*'
```

Expected: all unit tests pass: thresholds (RPE 8.0 and just above, exactly 5%, exactly 70%, 2 sessions a week), priority, open plan change, cool-down, once per week across time zones, daylight saving and a missed run, with a fixed clock.

```bash
./gradlew check
```

Expected: build succeeds with `HexagonalArchitectureTest` and the `*IT` test of the JDBC adapter (history and weekly claim on PostgreSQL) passing.

## Manual (local profile)

1. `./gradlew bootRun`, which starts PostgreSQL and loads the demo data.
2. In the database (`localhost:15432`, database `kinetiq`) set `weekly_checks` aside, then make a demo client's logs match a rule, for example set every `set_logs.rpe` of the last two completed weeks to 9.
3. Wait up to 10 minutes after the client's Monday 03:00, or restart the app after Monday 03:00. The first sweep runs at startup, so a restart is the fast way.
4. Expect one log line naming the rule `HIGH_RPE`, without any client data (no plan change is stored until task #132) and one `weekly_checks` row for the week; a second run in the same week adds nothing.

## Security check

- `/v3/api-docs` shows no new path.
- The log lines of a run contain rule names and counts, no names or emails.
- Two overlapping runs of the same week check the client once (covered by the integration test on the weekly claim).
