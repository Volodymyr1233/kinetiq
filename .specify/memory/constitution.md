# Kinetiq Constitution

## Core Principles

### I. Hexagonal Boundaries

- Domain code MUST NOT depend on frameworks (Spring, JPA, Jackson, MapStruct) or on infrastructure code.
- Adapters depend on ports; ports never depend on adapters. Input adapters and output adapters MUST NOT use each other.
- A feature's infrastructure is private to that feature. Features MUST NOT form dependency cycles.
- These rules are executable: `HexagonalArchitectureTest` MUST pass, and MUST be extended when a new rule is agreed.

Rationale: business rules stay testable without a container, and features can be built in parallel without stepping on each other.

### II. One Contract Between Frontend and Backend

- The backend's OpenAPI document, generated from code, is the only source of API types. Frontend types are generated from it and never written by hand.
- An API change and the regenerated `schema.d.ts` MUST land in the same pull request.
- Errors are RFC 9457 problem details carrying a stable machine-readable `code`. The frontend translates by `code` and MUST NOT display backend text.

Rationale: without CI, drift between the two halves is only prevented by making it impossible to merge one without the other.

### III. Secure by Default

- Every endpoint requires authentication unless explicitly opened. New public endpoints MUST be justified in the spec.
- Authentication is a server session cookie with CSRF protection, same origin. Tokens MUST NOT be stored in browser storage.
- Secrets live in environment variables only and MUST NOT be committed.
- Error responses MUST NOT expose exception messages, SQL, class names or stack traces.
- Prompts sent to an LLM MUST NOT contain names, emails or other identifiers of a person.

### IV. Explainable, Approved Plan Changes

- Whether a plan change is needed is decided by deterministic rules over logged data, never by an LLM.
- An LLM may draft the content of a plan change. The draft MUST pass domain bounds before anyone sees it, and takes effect only after trainer and client both accept it.
- Adjustments applied without approval (today-only targets after a low readiness check-in) MUST be rule-based.
- Every change made by the system MUST show the old value, the new value and the reason, and MUST be reversible.

Rationale: training load affects injury risk; a number a person trains with has to be traceable to a rule and a decision.

### V. Tests Verify Behaviour

- Tests MUST verify business behaviour and important edge cases, and MUST fail when that behaviour breaks.
- Tests that only exercise getters, constructors, trivial delegation or implementation details MUST NOT be written.
- Tests MUST be deterministic, independent of each other, and readable.
- The aim is high coverage of important logic, not a coverage percentage.

### VI. Simplicity

- Build the smallest thing that satisfies the spec. No abstraction without a second use, no configuration for a value that does not change.
- A dependency is added in the pull request that first needs it, not before.
- Complexity that violates this principle MUST be justified in the plan.

### VII. One Design System

- UI is built from the Kinetiq tokens and shadcn components. Raw palette classes and hand-rolled replacements for existing components MUST NOT be used.
- Every user-facing string exists in English and Polish.
- A UI change is done only after it was checked in a real browser, in light and dark, at the viewport of its audience (375px client, 1280px trainer).

## Technology Constraints

- Backend: Java 25, Spring Boot 4 (Spring MVC, virtual threads), Spring Data JPA, Flyway, PostgreSQL, Spring Security, MapStruct, Lombok.
- Frontend: React 19, TypeScript, Vite, Tailwind CSS 4, shadcn/ui, React Router, TanStack Query, react-i18next.
- Backend and frontend are separate builds in one repository. The frontend talks to the backend only through `/api/v1`.
- Schema changes are Flyway migrations. Hibernate validates the schema and never changes it.

## Development Workflow

- Work starts from a GitHub issue: a task of a story, a chore or a bug. Either one pull request per issue with one squash commit, or, when a whole story is built at once, one pull request for the story with one commit per task. See `CLAUDE.md`.
- Spec Kit is recommended for stories with domain logic. A spec, when written, is reviewed in the story's first pull request.
- Before a pull request is opened, `./gradlew check` (backend) and `npm run check` (frontend) MUST pass locally for the parts touched.
- Commits and pull request titles follow Conventional Commits.

## Governance

- This constitution overrides other practices where they conflict. `CLAUDE.md`, `backend/CLAUDE.md` and `frontend/CLAUDE.md` hold the day-to-day conventions that implement it.
- Amendments are made by pull request that changes this file and bumps the version: MAJOR for removing or redefining a principle, MINOR for adding one, PATCH for clarifications.
- Plans MUST include a constitution check; reviewers verify it.

**Version**: 1.1.0 | **Ratified**: 2026-10-11 | **Last Amended**: 2026-10-18
