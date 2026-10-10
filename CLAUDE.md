# Kinetiq

Training system for two roles. Trainers design plans, approve changes and watch progress, mostly on desktop. Clients check in, log sets and message their trainer, mostly on a phone.

- Domain vocabulary: `CONTEXT.md`. Use these terms in code, specs, UI copy and commits. Add a term there when a new one is agreed.
- Non-negotiable principles: `.specify/memory/constitution.md`.
- Details per side: `backend/CLAUDE.md`, `frontend/CLAUDE.md`.

## Layout

```
backend/     Spring Boot REST API (standalone Gradle project)
frontend/    React SPA (standalone npm project)
specs/       Spec Kit feature folders: specs/<issue>-<slug>/
docs/adr/    Architecture decision records (created when the first one is needed)
compose.yaml Local PostgreSQL, started automatically by the backend
```

The repository root has no build. Run backend commands in `backend/`, frontend commands in `frontend/`.

## Architecture

- The SPA calls the backend only through `/api/v1`, on the same origin: Vite proxies `/api` in development, a reverse proxy does it in production. There is no CORS configuration.
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

1. Work starts from a GitHub issue. Read it with `gh issue view <n>`.
2. Branch from the default branch: `<type>/<issue>-<slug>`, e.g. `feat/42-set-logging`.
3. Pick the Spec Kit path by size:
   - Substantial feature: `/speckit-specify` → `/speckit-clarify` → `/speckit-plan` → `/speckit-checklist` → `/speckit-tasks` → `/speckit-analyze` → `/speckit-implement` → `/speckit-converge`
   - Small feature: `/speckit-specify` → `/speckit-plan` → `/speckit-tasks` → `/speckit-implement` → `/speckit-converge`
   - Chore or bug fix: no Spec Kit.
4. Name the spec folder after the issue: `specs/<issue>-<slug>`. Spec Kit does not create branches here.
5. One pull request per issue, containing spec, plan, tasks and code. Squash merge. The pull request closes the issue (`Closes #<n>`).

`/speckit-taskstoissues` is not used: tasks stay as checkboxes in `tasks.md`.

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
- Comments explain why, not what. No commented-out code.
- Do not edit generated or tool-managed files by hand: `frontend/src/lib/api/schema.d.ts`, `.specify/scripts/*`, `.specify/templates/*`.

## Decisions

Record a decision in `docs/adr/NNNN-slug.md` only when it is hard to reverse, surprising without context, and the result of a real trade-off. One paragraph is enough.
