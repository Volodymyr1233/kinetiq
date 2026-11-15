# Implementation Plan: Plan change takes effect or expires

**Branch**: `feat/143-plan-change-effect-expiry` | **Date**: 2026-11-13 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/143-plan-change-effect-expiry/spec.md`

## Summary

A scheduled step ends every plan change whose effective Monday has begun in the client's time zone. One that both people accepted is applied to the plan: the plan feature stores the plan as it was (a revision, valid until that Monday) and replaces its sessions with the result of the content. One that is still open, or whose content no longer fits the current plan, expires. The plan change records when it took effect or why it expired. Reads follow: a past week of the plan returns the stored revision, the client's and trainer's plan screens show the change accepted by both before its Monday, and the plan change screens show the new endings (in effect, expired because undecided, expired because the plan was edited). There is no new write endpoint: the step is internal.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.1; TypeScript 5, React 19.

**Primary Dependencies**: existing only (Spring `@Scheduled`, JdbcTemplate, JPA, Flyway, MapStruct, Lombok; react-query, react-i18next, shadcn). No new dependency.

**Storage**: PostgreSQL; migration `V29__add_plan_change_effect.sql` (re-check the number against `master` and open pull requests before the pull request).

**Testing**: JUnit 5 + AssertJ with hand-written fakes and a fixed `Clock` (domain), `@DataJpaTest` + Testcontainers (`*IT`), `@WebMvcTest`, Vitest + Testing Library; a final browser check of every screen and state.

**Target Platform**: Linux server (one image serves the SPA); client UI 375px, trainer UI 1280px.

**Project Type**: web application (backend + frontend).

**Performance Goals**: one indexed query finds the due plan changes; each is handled in its own transaction; a change reaches the plan within 5 minutes of its Monday beginning.

**Constraints**: applying and expiring are idempotent and catch up after downtime; plan revision and plan change update are one transaction; no lost acceptance at the boundary; no backend text in the UI; no LLM involved.

**Scale/Scope**: at most one open or upcoming plan change per client; tens of clients per trainer.

## Constitution Check

*GATE: passes before research and after design.*

| Principle | Check |
|---|---|
| I Hexagonal | `planchange` reaches the plan feature only through a new `plan.domain.input` port (`RevisePlanUseCase`); `plan` never imports `planchange`, so no cycle (`overview` already depends on `planchange`). The sweep is an input adapter (precedent `PlanChangeSweep`, `PlanDraftingSweep`). The due query is an output port with a JDBC adapter. `HexagonalArchitectureTest` stays green. |
| II One contract | New and changed responses come from code; `schema.d.ts` is regenerated in the same pull request; refusals keep stable codes; the UI translates by code. |
| III Secure by default | The step has no endpoint. New read endpoints are authenticated, role-scoped, GET only, ownership checked, `404` for another person's id. No personal data in logs; no text a person wrote is sent anywhere. |
| IV Explainable, approved | Applying needs both acceptances and the clock only; the revision keeps the old plan; the plan change keeps old value, new value and reason; the trainer can reverse by editing the plan. No LLM. |
| V Tests verify behaviour | Fixed-clock tests for apply, expire, plan edited, late and repeated runs, time zones, past weeks, the boundary race; `404` per endpoint; persistence IT for the constraints. |
| VI Simplicity | No new state: `APPLIED` + `tookEffectAt`; `EXPIRED` + a cause. History is one snapshot per applied change, not versioning of every edit (ceiling in research). Upcoming values are read from the plan change, not stored twice. |
| VII Design system | Tokens and shadcn components only; strings in `en.json` and `pl.json`; checked in a real browser, light and dark, at 375px (client) and 1280px (trainer). |

No violations; Complexity Tracking is empty.

## Project Structure

### Documentation (this feature)

```text
specs/143-plan-change-effect-expiry/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/api.md
├── checklists/requirements.md
└── tasks.md          # /speckit-tasks
```

### Source code

```text
backend/src/main/
├── resources/db/migration/V29__add_plan_change_effect.sql
├── resources/db/dev-data/R__demo_plan_changes.sql      # upcoming, in effect, expired
└── java/com/kinetiq/
    ├── plan/
    │   ├── domain/model/     PlanRevision (the sessions as they were, valid before a Monday)
    │   ├── domain/input/     RevisePlanUseCase (client, expected version, effective Monday, sessions)
    │   ├── domain/output/    PlanRepository (+ saveRevision, revisions of a plan)
    │   ├── domain/service/   PlanRevisionService; PlanViewService reads the revision for a past week
    │   └── infrastructure/   revision entities, repository impl
    └── planchange/
        ├── domain/model/     PlanChange (+ takeEffect, expire, tookEffectAt, ExpiryCause); still-valid check on PlanSnapshot
        ├── domain/input/     ApplyPlanChangesUseCase (due, settle); views gain tookEffectAt and cause; upcoming views
        ├── domain/output/    PlanChangeRepository (+ findDue; hasOpen counts an upcoming change)
        ├── domain/service/   PlanChangeSettlementService; detection treats upcoming as open
        └── infrastructure/   PlanChangeEffectSweep (input), entity columns, responses, upcoming endpoints
backend/src/test/...          unit tests with a fixed clock, WebMvc tests, ITs

frontend/src/
├── features/plan/            client plan: upcoming marks from the effective week, "changed by a plan change" note; trainer editor banner
├── features/planchange/      state line and rows: applied (upcoming / in effect), expired causes; hooks for the upcoming change
├── lib/plan-change-state.ts  labels
└── locales/                  en.json, pl.json
```

**Structure Decision**: the existing hexagonal layout by feature on the backend and the feature folders on the frontend; the only cross-feature call is `planchange` → `plan.domain.input`.

## Task outline (commit per task; #143 stays the story)

| Issue | Content |
|---|---|
| #144 Apply or expire at the effective Monday | migration; `PlanChange.takeEffect` / `expire` with cause; still-valid check; `RevisePlanUseCase` and revision storage in `plan`; settlement service; due query; `PlanChangeEffectSweep`; detection counts an upcoming change as open; fixed-clock tests for both outcomes, late and repeated runs, time zones, plan edited |
| #145 Upcoming and applied change on the plan screens | plan view returns the revision for past weeks; upcoming endpoints (client, trainer) and the client's applied endpoint; client plan marks and note, trainer editor banner; Approvals and client page row labels; regenerated `schema.d.ts` |
| #146 Expired state on both screens | expiry cause in the responses; client screen and trainer detail show the two sentences; rows and counts; strings |

Dependencies: #144 → #145 → #146 (the screens need the stored fields; #146 reads the cause #144 writes).

## Final verification (part of Definition of Done, not optional)

- `./gradlew check` and `npm run check` pass.
- Quickstart scenarios run against a real database and in the built-in browser: client 375px and trainer 1280px, light and dark, English and Polish, keyboard and labels; console clean.
- Security pass over the new endpoints (role, ownership `404`, no write path, no leakage), result written in the pull request.

## Complexity Tracking

None.
