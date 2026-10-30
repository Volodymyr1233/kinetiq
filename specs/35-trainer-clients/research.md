# Research: Client list API (#36)

## Where the list lives

- **Decision**: a new feature `client`.
- **Rationale**: the entry joins an account (feature `account`) and the quiz goal (feature `quiz`). `quiz` depends on `account` (`AccountId`), so a listing in `account` that needs `quiz` would form a cycle, which the architecture test refuses. `client` depends on both and nothing depends on it.
- **Alternatives**: put it in `quiz` (wrong name for a client list); give `account` and `quiz` each a port and join in the service (three features touched and several ports for one screen).

## How the data is read

- **Decision**: one SQL query in a `JdbcClientDirectory` adapter that selects from `accounts` left join `quiz_submissions`, filtered by `accounts.trainer_id`.
- **Rationale**: a list is a read model over several tables; one query avoids a call per client. It uses no class of another feature, so the architecture rules hold. When plans and logs exist, the same query gains joins.
- **Trade-off accepted**: the adapter knows two other features' table layouts, so a change to those tables must check this query. The adapter test (`JdbcClientDirectoryIT`) runs against the real migrations and fails when they drift.
- **Alternatives**: `JpaRepository` projection (needs the other features' entities, which are private to them); the existing `AccountRepository.findClientsOf` plus one more call per client (N+1).

## Plan state before plans exist

- **Decision**: `NO_QUIZ` when the client has no quiz submission, `PLAN_TO_REVIEW` when they have. The enum grows with the plan stories (week N of M, finished).
- **Rationale**: matches the story's list of states, and what the board shows today; the draft plan the trainer must review is the one built from the quiz (CONTEXT.md, Draft).
- **Alternatives**: return only `hasQuiz`; rejected because the screen speaks in plan states and the contract should not change its meaning later.

## Columns

- **Decision**: the list returns `id`, `name`, `goal`, `planState`; `week`, `weeks` and `lastLogAt` are added by the stories that produce them.
- **Rationale**: no field that is always empty; the screen shows empty columns for what is not there yet (spec FR-003).

## Access

- **Decision**: `/api/v1/clients/**` is `hasRole("TRAINER")`, next to exercises and invites; the query filters by the signed-in id.
- **Rationale**: role gate for a clear `403` to a client; ownership in the query so another trainer's clients cannot appear even if the gate is changed.

## Sorting and search

- **Decision**: `order by lower(name), id`; no search parameter.
- **Rationale**: the table sorts by name without regard to case, and the search is a filter over the list already read (spec Assumptions).
