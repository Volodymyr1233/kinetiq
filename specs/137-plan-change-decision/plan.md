# Implementation Plan: Trainer and client decide on a plan change

**Branch**: `feat/137-plan-change-decision` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/137-plan-change-decision/spec.md`

## Summary

A stored pending plan change (story #131) gets a decision flow. The `PlanChange` aggregate learns `accept`, `undo` and `decline`, with who accepted kept as two timestamps and the state derived from them. A decision service checks ownership (trainer through the client feature, client by id), the effective Monday, and applies the transition under a row lock. Keeping the current plan stores reason and note and posts one message into the conversation through the messaging feature's input port, in the same transaction. Notification is derived state (no new store). The trainer gets decision endpoints, a plan change list for Approvals and a client-page history; the client gets read and decision endpoints. The frontend adds the client plan change screen (375px) with Plan and Today cards, the trainer Approvals rows, the decision actions on the detail, and checks the existing chart against the design system. Applying, expiring and showing the upcoming change are story #143.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.1; TypeScript 5, React 19.

**Primary Dependencies**: existing only (Spring MVC, Spring Data JPA, JdbcTemplate, Flyway, MapStruct, Lombok; react-query, react-i18next, shadcn `dialog`/`sheet`/`badge`/`card`/`table`, Recharts through shadcn `chart`). Any shadcn component not yet installed is added through the CLI.

**Storage**: PostgreSQL; migration `V25__add_plan_change_decisions.sql` (re-check numbering before the pull request).

**Testing**: JUnit 5 + AssertJ with hand-written fakes (domain), `@DataJpaTest` + Testcontainers (`*IT`), `@WebMvcTest` for controllers, Vitest + Testing Library; a final browser check of every screen and state.

**Target Platform**: Linux server (one image serves the SPA); client UI at 375px, trainer UI at 1280px.

**Project Type**: web application (backend + frontend).

**Performance Goals**: a decision answers within 1 s locally; Approvals loads one extra query.

**Constraints**: one transaction per decision (including the message); no lost acceptance under concurrency; no backend text in the UI; notes are plain text.

**Scale/Scope**: one open plan change per client; tens of clients per trainer.

## Constitution Check

*GATE: passes before research and after design.*

| Principle | Check |
|---|---|
| I Hexagonal | All new code in `planchange` (`domain.model`, `input`, `output`, `service`, `exception`; `infrastructure.input`, `output`, `config`). The decline reaches messaging only through `messaging.domain.input.ExchangeMessagesUseCase`; messaging never depends on planchange, so no cycle. Reads for lists are an output port with a JDBC adapter (precedent: `JdbcPlanSource`). `HexagonalArchitectureTest` stays green. |
| II One contract | New endpoints in the generated OpenAPI; `schema.d.ts` regenerated in the same pull request (once, with the first task that changes the API, and again as later tasks add more); errors are problem details with stable codes; the UI translates by code. The server-built `title` and `reason` are the only backend text shown (required by the story). |
| III Secure by default | Every endpoint authenticated; trainer paths already trainer-only, client paths under `/api/v1/plan/**` already client-only; ownership checked in the use case; another person's resource → `404`; CSRF on every `POST`/`DELETE`; note validated (reason enum, ≤ 500) and shown as plain text; the note is never sent to an LLM; no personal data in logs. |
| IV Explainable, approved | A change takes effect only after both accept (this story records it; #143 applies it); old value, new value and reason stay visible on both screens; acceptance is reversible until the other accepts; nothing here involves an LLM. |
| V Tests verify behaviour | Every transition (allowed and refused) as unit tests; concurrency of two accepts; decline posts one message and is idempotent; `404` per endpoint; persistence IT for the decision constraints; frontend tests for each state and for the keep form. |
| VI Simplicity | Two timestamps instead of a decision log; no notification store; one decision use case for both sides; one sheet/dialog form; Approvals tabs built on the frontend from two lists. |
| VII Design system | Tokens and shadcn components only; Accept is not the lime button; strings in `en.json` and `pl.json`; checked in a real browser, light and dark, at 375px (client) and 1280px (trainer). |

No violations; Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/137-plan-change-decision/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── api.md
├── checklists/requirements.md
└── tasks.md          # /speckit-tasks
```

### Source code

```text
backend/src/main/
├── resources/db/migration/V25__add_plan_change_decisions.sql
├── resources/db/dev-data/R__demo_plan_changes.sql          # more states
└── java/com/kinetiq/planchange/
    ├── domain/
    │   ├── model/       Side, DeclineReason, Decision; PlanChange (accept/undo/decline, acceptance times)
    │   ├── input/       DecidePlanChangeUseCase, ViewOwnPlanChangeUseCase, ViewPlanChangeUseCase (+ ended, approvals), views
    │   ├── output/      PlanChangeRepository (+ findForUpdate, update, findEnded), PlanChangeApprovals
    │   ├── service/     PlanChangeDecisionService, ClientPlanChangeViewService, DeclineMessageWording
    │   └── exception/   PlanChangeEndedException, PlanChangeNothingToUndoException, PlanChangeWeekBegunException
    └── infrastructure/
        ├── config/      wiring of the new services (+ ExchangeMessagesUseCase, Clock)
        ├── input/       PlanChangeController (+ decisions, approvals), ClientPlanChangeController, requests/responses, web mapper
        └── output/      entity columns, mapper, repository impl (lock), JdbcPlanChangeApprovals
backend/src/test/...     unit tests, WebMvc tests, PlanChangeRepositoryIT, architecture test unchanged

frontend/src/
├── features/planchange/
│   ├── api/             decisions (accept, undo, keep), client plan change hooks
│   ├── components/      decision panel, keep-current form (sheet/dialog), plan change state line, rule chart (compact)
│   └── pages/           plan-change-page (trainer, decisions added), client-plan-change-page (new)
├── features/plan/       approvals-page (rows + tabs), api/client-plan-change.ts (cards on Plan and Today), nav count
├── features/client/     client page: open plan change state + last 5 ended
├── components/          plan-change-card (state variants)
├── app/                 router: /app/plan/change/:id; trainer-area nav count
└── locales/             en.json, pl.json
```

**Structure Decision**: the existing hexagonal layout by feature on the backend and the feature folders on the frontend; no new module. Backend work is within `planchange`; the only cross-feature call is the messaging input port.

## Task outline (commit per task; #137 stays the story)

| Issue | Content |
|---|---|
| #138 Plan change states and decision API | migration; aggregate transitions and exceptions; decision service; repository lock; trainer and client endpoints; ownership and `404` tests; regenerated `schema.d.ts` |
| #139 Decline reason posted into the conversation | `DeclineMessageWording`; decision service posts through `ExchangeMessagesUseCase` in the same transaction; idempotent id; tests for both conversation views |
| #140 Client plan change screen | client read endpoints (`current`, by id); screen, sheet, state variants; cards on Plan and Today; strings |
| #141 Trainer plan change list and detail | approvals and ended endpoints; Approvals rows, tabs and nav count; detail actions; client page state and history; strings |
| #142 Why-this-change chart | check `RuleChart` against the design system's chart rules; compact variant for the client; "Same weeks" list if absent; chart tests |

Dependencies: #138 → #139 → (#140, #141 in either order) → #142. API additions for #140/#141 regenerate `schema.d.ts` in their own commits.

## Final verification (part of Definition of Done, not optional)

- `./gradlew check` and `npm run check` pass.
- Quickstart scenarios run in the built-in browser: client 375px and trainer 1280px, light and dark, English and Polish, keyboard and labels; console clean.
- Security pass over the new endpoints (role, ownership `404`, CSRF, validation, no leakage) with a written result in the pull request.

## Complexity Tracking

None.
