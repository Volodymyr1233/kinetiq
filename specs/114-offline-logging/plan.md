# Implementation Plan: Client logs without a connection

**Branch**: `feat/114-offline-logging` | **Date**: 2026-11-09 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/114-offline-logging/spec.md`

## Summary

Set logs made without a connection are kept as paused TanStack Query mutations, written to `localStorage` and replayed when the app is open and online (ADR 0003). The work is frontend only: the set-log endpoint already stores a repeated client id once, refuses a locked session and answers "not found" for another client's session (`SessionLogServiceTest`), so no API changes and no `schema.d.ts` regeneration. Backend tasks are tests that pin the replay cases. Task #115 builds the persisted queue and the replay; task #116 builds the banner, the per-row state, Retry, Discard and the disabled Finish.

## Technical Context

**Language/Version**: TypeScript, React 19 (frontend); Java 25 / Spring Boot 4 (backend, tests only)

**Primary Dependencies**: TanStack Query 5 (installed); adds `@tanstack/react-query-persist-client` and `@tanstack/query-sync-storage-persister` in the PR that first needs them

**Storage**: browser `localStorage`, one key per account; no server storage change

**Testing**: Vitest with Testing Library (frontend); JUnit (backend replay cases); manual check in the built-in browser with offline mode

**Target Platform**: phone browser, client area at 375 px

**Project Type**: web application (frontend + backend)

**Performance Goals**: the logger never waits on storage

**Constraints**: no service worker, opening the app offline unsupported (ADR 0003); no tokens or personal identifiers in storage; sync storage must not crash the logger when unavailable

**Scale/Scope**: a session holds a few dozen sets, a few kilobytes

## Constitution Check

| Principle | Result |
|---|---|
| I Hexagonal | No backend structure change. Pass. |
| II One contract | No API change; no hand-written API types. Kept values reuse `LogSetBody`. Pass. |
| III Secure by default | No new endpoint. Storage holds set values and ids only, no tokens, names or emails; keyed per account and cleared on sign-out; a replay is still authorised by the server's session cookie and ownership. Pass. |
| IV Plan changes | Not touched. Pass. |
| V Tests verify behaviour | Tests cover replay, no duplicates, account isolation, state per row, retry, discard. Pass. |
| VI Simplicity | Reuse TanStack's persistence rather than a hand-written queue; no IndexedDB, no service worker, no cross-tab lock. Two small dependencies, justified in research. Pass. |
| VII Design system | Banner and row states from shadcn `Alert`, `Badge`, `Button` and tokens; both languages; browser check. Pass. |

Post-design re-check: unchanged, pass.

## Project Structure

### Documentation (this feature)

```text
specs/114-offline-logging/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/pending-logs-storage.md
└── tasks.md             # /speckit-tasks
```

### Source Code

```text
frontend/src/
├── lib/
│   ├── online.ts                  # useOnline
│   └── pending-logs.ts            # per-account persister, restore, clear
├── app/pending-logs-route.tsx     # registers log-set defaults, mounts the persisting provider around every client route
├── features/sessionlog/
│   ├── api/session-log.ts         # useLogSet keyed, pending read, retry, discard
│   ├── components/
│   │   ├── offline-banner.tsx
│   │   ├── set-row.tsx            # state, Retry, Discard
│   │   └── finish-footer.tsx      # disabled until synced
│   └── pages/logger-page.tsx      # banner
└── locales/{en,pl}.json

backend/src/test/java/com/kinetiq/sessionlog/…   # replay cases, only where missing
```

**Structure Decision**: Existing web layout. The persister is app-wide plumbing in `lib/`; everything about rows and banners stays in `features/sessionlog`.

## Task split (one commit each, one pull request)

1. **#115 Pending set logs kept and replayed**: dependencies, mutation defaults and retry rule, per-account persistence, restore and resume, sign-out and account-switch clearing, 401 handling, backend replay tests.
2. **#116 Offline banner, per-log status and retry**: banner, row state from the mutation cache, Retry, Discard, Finish disabled, translations, browser and backend verification.

## Complexity Tracking

No violations.
