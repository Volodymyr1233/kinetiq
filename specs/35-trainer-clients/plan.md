# Implementation Plan: Trainer sees their clients

**Branch**: `feat/36-client-list-api` | **Date**: 2026-10-30 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/35-trainer-clients/spec.md`

## Summary

Task #36 adds `GET /api/v1/clients`: a new backend feature `client` with a `ViewClientsUseCase`, a `ClientDirectory` output port and one SQL read model that joins the trainer's clients with their quiz answers. The route is opened to trainers only. #37 and #38 (frontend, and the single-client read) are planned when they start.

## Technical Context

**Language/Version**: Java 25, Spring Boot 4.1.1

**Primary Dependencies**: Spring MVC, Spring JDBC (`JdbcClient`, already on the classpath), Lombok

**Storage**: PostgreSQL; reads `accounts` and `quiz_submissions`, no new table, no migration

**Testing**: JUnit 5, AssertJ, `@WebMvcTest` (controller), `@JdbcTest` with Testcontainers (adapter, `*IT`), ArchUnit

**Target Platform**: Linux server

**Project Type**: web service (backend in this task)

**Performance Goals**: one query per request, no per-client queries

**Constraints**: ownership by the signed-in trainer in the query; no personal data beyond the columns of FR-002; error bodies as problem details

**Scale/Scope**: tens of clients per trainer; no paging

## Constitution Check

| Principle | Result |
|---|---|
| I. Hexagonal boundaries | Pass. `client.domain` is framework-free; the SQL lives in `client.infrastructure.output`; nothing in `account` or `quiz` depends on `client`. The adapter reads two other features' tables (see research); it uses none of their classes. |
| II. One contract | Pass. A new endpoint and response; `schema.d.ts` regenerated in the same pull request. |
| III. Secure by default | Pass. `/api/v1/clients/**` is opened to the `TRAINER` role only; the query filters by the signed-in trainer's id; no new public endpoint. |
| IV. Approved plan changes | Not applicable. |
| V. Tests verify behaviour | Pass. Adapter test for ownership, order, goal and state; controller test for roles and shape. |
| VI. Simplicity | Pass. No paging, no search parameter, no empty columns for later; one SQL query. |
| VII. One design system | Not applicable in this task (no UI). |

Re-checked after design: unchanged.

## Project Structure

### Documentation (this feature)

```text
specs/35-trainer-clients/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/clients-api.md
├── checklists/requirements.md
└── tasks.md
```

### Source Code (repository root)

```text
backend/src/main/java/com/kinetiq/client/
├── domain/
│   ├── model/        ClientSummary, PlanState
│   ├── input/        ViewClientsUseCase
│   ├── output/       ClientDirectory
│   └── service/      ClientViewService
└── infrastructure/
    ├── config/       ClientConfig
    ├── input/        ClientController, ClientListResponse, ClientResponse, ClientWebMapper
    └── output/       JdbcClientDirectory

backend/src/main/java/com/kinetiq/common/infrastructure/config/SecurityConfig.java   (route rule)
frontend/src/lib/api/schema.d.ts                                                      (regenerated)

backend/src/test/java/com/kinetiq/client/
├── infrastructure/input/ClientControllerTest.java
└── infrastructure/output/JdbcClientDirectoryIT.java
```

**Structure Decision**: the standard feature layout of `backend/CLAUDE.md`. A new feature, not part of `account`, because the goal comes from `quiz` and `quiz` already depends on `account`; a listing inside `account` would close a cycle.

## Complexity Tracking

No violations.
