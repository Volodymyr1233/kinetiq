# Kinetiq

Training system for two roles. Trainers design plans, approve changes and watch progress, mostly on desktop. Clients check in, log sets and message their trainer, mostly on a phone.

- Domain vocabulary: `CONTEXT.md`. Use these terms in code, specs, UI copy and commits. Add a term there when a new one is agreed.
- Non-negotiable principles: `.specify/memory/constitution.md`.
- Details per side: `backend/CLAUDE.md`, `frontend/CLAUDE.md`.

## Layout

```
backend/     Spring Boot REST API (standalone Gradle project)
frontend/    React SPA (standalone npm project)
specs/       Optional Spec Kit folders per story: specs/<story>-<slug>/
docs/adr/    Architecture decision records
compose.yaml Local PostgreSQL, started automatically by the backend
```

The repository root has no build. Run backend commands in `backend/`, frontend commands in `frontend/`.

## Architecture

- The SPA calls the backend only through `/api/v1`, on the same origin: Vite proxies `/api` in development; in production the backend serves the built SPA from the same image (`docs/adr/0001`). There is no CORS configuration.
- Authentication is a server session cookie with CSRF protection. No tokens in browser storage.
- The backend is hexagonal and organised by feature. The frontend is organised by the same features.
- The API contract is the backend's generated OpenAPI document. Frontend types are generated from it (`npm run gen:api`) and committed. An API change and the regenerated types go in the same pull request.

## Commands

| What | Where | Command |
|---|---|---|
| Run backend (starts PostgreSQL) | `backend/` | `./gradlew bootRun` |
| Backend quality gate | `backend/` | `./gradlew check` |
| Run frontend | `frontend/` | `npm run dev` |
| Frontend quality gate | `frontend/` | `npm run check` |
| Regenerate API types (backend running) | `frontend/` | `npm run gen:api` |

There is no CI. Run the quality gate of every side you touched before committing, and report its real result.

## Workflow

1. Work starts from a GitHub issue. A story (label `story`) is split into task sub-issues (label `task`); chores and bugs stand alone. Read it with `gh issue view <n>`, including its parent story and its "blocked by" links. Do not start a task whose blockers are still open.
2. Branch from `master`: `<type>/<issue>-<slug>`, where the issue is the task, chore or bug, e.g. `feat/42-set-logging`.
3. One pull request per task, chore or bug. It closes that issue (`Closes #<n>`). Squash merge. A story closes when its last task does.
4. Spec Kit is recommended for stories with domain logic, not required. Whoever starts such a story may write its spec in the first task's pull request, in `specs/<story>-<slug>`; later tasks of the story build on it.
   - Substantial story: `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-checklist` → `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement` → `/speckit-converge`
   - Small story: `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement` → `/speckit-converge`
   - Spec Kit does not create branches here, and `/speckit-taskstoissues` is not used: issues are written by people.
5. Work found during a task that is out of its scope becomes a new task under the same story (`task` issue form), not a bigger pull request.

## Definition of Done

Every task, chore and bug fix, in addition to its own acceptance criteria:

- The quality gate of every side touched passes (`./gradlew check`, `npm run check`).
- Behaviour is covered by tests as described in `backend/CLAUDE.md` and `frontend/CLAUDE.md`.
- Another person's resource answers `404`, and a test proves it.
- Every new user-facing string exists in English and Polish.
- UI changes were checked in the browser, light and dark, at 375px (client) or 1280px (trainer), including keyboard focus and labels (WCAG 2.2 AA).
- An API change comes with the regenerated `schema.d.ts`.
- New entities get demo data in the local-only Flyway location.

## Commits and pull requests

[Conventional Commits](https://www.conventionalcommits.org/): `<type>(<scope>): <subject>`

- Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `perf`, `build`.
- Scopes: `backend`, `frontend`, `docs`, `deps`. Omit the scope when a change spans both sides.
- Subject: imperative, lower case, no full stop, at most 72 characters.
- Body: only when the subject does not say enough, and then a few short bullet points.
- No `Co-Authored-By` or other attribution footers.
- The pull request title follows the same format; it becomes the squash commit.

## Tools

- **context7 MCP**: before using or configuring any library, framework or CLI, fetch its current documentation through context7, without being asked. This stack is newer than most training data (Spring Boot 4, Spring Security 7, React Router 8, Tailwind 4, shadcn CLI 4).
- **gh**: all GitHub reads and writes (issues, pull requests).
- **shadcn CLI**: adding and updating UI components. See `frontend/CLAUDE.md`.
- **Built-in browser**: verifying UI changes. See `frontend/CLAUDE.md`.

## Security

- Every endpoint requires authentication unless deliberately opened. Never open one to make something work.
- Secrets come from environment variables. Never commit them, never print them, never read `.env`.
- Never put exception messages, SQL, class names or stack traces in an API response.
- Never send names, emails or other personal identifiers to an LLM.
- Validate input at the boundary: Bean Validation on request bodies, invariants in domain constructors.
- Authorise by ownership, not only by role: a trainer sees only their clients, a client only their own data. Answer `404`, not `403`, for another person's resource.

## Conventions for both sides

- Code, comments, commits, specs and docs are in English. The UI ships in English and Polish.
- Build the smallest thing that satisfies the issue. No abstractions, options or dependencies for later.
- Comments explain why, not what: never state the obvious or repeat the code. Keep them short, preferably one line, starting with a lower-case letter (unless it begins with a proper noun or technical name) and without a closing full stop. No commented-out code.
- Do not edit generated or tool-managed files by hand: `frontend/src/lib/api/schema.d.ts`, `.specify/scripts/*`, `.specify/templates/*`.

## Decisions

Record a decision in `docs/adr/NNNN-slug.md` only when it is hard to reverse, surprising without context, and the result of a real trade-off. One paragraph is enough.
