# Research: Client logs without a connection

## Decisions

### 1. Keep the queue as TanStack Query mutations, persisted to localStorage

- **Decision**: One mutation key `['log-set']`, with its `mutationFn`, retry rule and cache update registered once through `queryClient.setMutationDefaults`; persisted with the query persistence packages; `resumePausedMutations()` runs after restore.
- **Rationale**: ADR 0003. With the default network mode a mutation made offline pauses by itself and runs when the browser is online again. A restored mutation only runs if its function comes from defaults, so callbacks that matter after a reload (updating the cached session) live in the defaults, not in `mutate()` options.
- **Alternatives**: hand-written queue in localStorage (more code, own ordering and retry logic); IndexedDB (ADR: async persister for nothing); service worker with background sync (a PWA, out of scope).

### 2. Persist mutations only, never queries

- **Decision**: `dehydrateOptions.shouldDehydrateQuery: () => false`; `shouldDehydrateMutation` keeps `log-set` mutations that are paused or failed (a failed one must survive for Retry and Discard, FR-001).
- **Rationale**: Cached session logs hold exercise names and plan data and are re-fetched anyway; storing them widens what sits on the phone (constitution III) and can show stale rows.
- **Alternatives**: persist the whole cache (stale data, more personal data on disk).

### 3. One storage key per account

- **Decision**: key `kinetiq.pending-logs.<accountId>`. On sign-out the key is removed; after a sign-in, keys of other accounts are removed. The provider is mounted in the client area, where the account is already known.
- **Rationale**: FR-005. Another account cannot read or replay the first account's sets, with no ownership field to compare.
- **Alternatives**: one key with an owner field (one mistake leaks across accounts).

### 4. Retry rule

- **Decision**: retry 3 times with exponential delay only when the error is not a definite refusal (`isWorthRetrying`: no `status`, or 5xx). A 4xx problem detail is final at once. A 401 is not a refusal: the set stays kept, the existing client redirects to sign-in, and the mutation is run again after a sign-in as the same account.
- **Rationale**: FR-012a, FR-005a. A network failure throws before any status exists.
- **Alternatives**: retry forever (a locked set would loop).

### 5. Row state is read from the mutation cache

- **Decision**: the row finds its pending mutation by key `['log-set', sessionId, position, setNumber]` (`useMutationState`). Pending or paused is Saved on this phone; error is Not synced; none with a stored log is Synced. A pending mutation's values replace the server's in the row, and its `setLogId` is reused, so logging the same place again after a reload sends the same id and never conflicts with an earlier kept send.
- **Rationale**: the existing row makes a random id per mount; after a reload that would send a second id for a place already kept, which the server refuses as "id in use".
- **Alternatives**: a separate store for row state (second source of truth).

### 6. Online state

- **Decision**: the banner and Finish read TanStack's `onlineManager` (browser online and offline events), through `useSyncExternalStore`.
- **Rationale**: the same signal pauses the mutations, so banner and queue never disagree.

### 7. Two tabs

- **Decision**: no cross-tab lock (spec FR-003a). Both tabs may send; the server stores once. A tab refetches the session when it regains focus.
- **Rationale**: duplicates are harmless and a lock needs a leader election.

### 8. Dependencies

- **Decision**: add `@tanstack/react-query-persist-client` and `@tanstack/query-sync-storage-persister`.
- **Rationale**: first needed here (constitution VI); they are the supported way to persist mutations. The sync persister suits `localStorage`.

### 9. Backend

- **Decision**: no change. Existing tests prove a repeated id stores once (`SessionLogServiceTest`), another client's session answers "not found", and a locked session refuses. Missing cases (a replay after the lock with new values, another client's set id) are added as tests in task #115.
- **Rationale**: the server already owns every rule (spec FR-009).

### 10. Storage failure

- **Decision**: every storage read and write is guarded; when storage is unavailable the persister does nothing, the queue works in memory for the open tab, and the banner adds a line that logs are not kept on the phone.
- **Rationale**: spec edge cases; private mode throws on write.

## Resolved unknowns

Spec 92 idempotency: confirmed by tests. No open NEEDS CLARIFICATION.
