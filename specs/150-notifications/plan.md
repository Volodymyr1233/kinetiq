# Implementation Plan: Trainer and client get in-app notifications

**Branch**: `feat/150-notifications` | **Date**: 2026-11-14 | **Spec**: [spec.md](spec.md)

## Summary

A new backend feature `notification` stores one row per notification and serves `GET /api/v1/notifications` and `POST /api/v1/notifications/{id}/read`. Features raise a notice through one port in the shared kernel (`common`), `NoticeBoard.raise`, which publishes a Spring event; `notification` listens, in the raising transaction, and stores it; after commit it publishes a second event the existing event stream (messaging) turns into an SSE event `notification`. A client's deletion removes its notifications through foreign keys that cascade. The frontend gets `features/notifications`: a bell with the count, a side sheet, toasts, and the Approvals count refreshed on the event.

## Technical Context

**Language/Version**: Java 25 (Spring Boot 4.1) backend; TypeScript, React 19 frontend

**Primary Dependencies**: Existing only (Spring MVC, Spring Data JPA, Flyway, MapStruct; TanStack Query, react-i18next, shadcn `sheet`, `button`, `badge`, `sonner`)

**Storage**: PostgreSQL, one new table `notifications`, migration after the last one on `master`

**Testing**: JUnit 5 + AssertJ (services with fakes), `@WebMvcTest`, Testcontainers adapter test, Vitest + Testing Library; `HexagonalArchitectureTest`

**Target Platform**: Browser, client 375 px, trainer 1280 px; same-origin `/api/v1`

**Constraints**: No feature cycles: sources depend on `common` only; `notification` listens and never calls back. Erasure by `on delete cascade`. Another person's notification answers 404.

## Constitution Check

| Principle | Check | Result |
|---|---|---|
| I. Hexagonal boundaries | `NoticeBoard` is a port in `common.domain.output` with a Spring adapter in `common.infrastructure`; `notification` has its own domain, ports and adapters; `common` holds only plain types (UUID), so no `common` to `account` cycle. | Pass |
| II. One contract | Two endpoints in the OpenAPI document, `schema.d.ts` regenerated in the same pull request; stable error codes. | Pass |
| III. Secure by default | `/api/v1/notifications/**` for any signed-in role; the person comes from the sign-in; another's is 404; a deletion notice holds no client. | Pass |
| IV. Explainable plan changes | Not touched; notifications only tell. | n/a |
| V. Tests verify behaviour | One test per type, own-only, erasure, stream delivery, bell and sheet behaviour. No getter tests. | Pass |
| VI. Simplicity | One port, one table, one stream; no preferences, no email, no paging beyond 30. | Pass |
| VII. One design system | Kinetiq tokens, shadcn `sheet`; EN and PL; checked in the browser. | Pass |

## Project Structure

```text
backend/src/main/java/com/kinetiq/
├── common/domain/{model/Notice*, output/NoticeBoard}.java      # shared kernel: UUIDs and an enum only
├── common/infrastructure/output/SpringNoticeBoard.java
├── notification/domain/{model,input,output,service,exception}
├── notification/infrastructure/{config,input,output}            # controller, listener, JPA adapter
├── messaging/infrastructure/input/ConversationStreams.java      # + event notification
└── (each source service calls NoticeBoard.raise)
backend/src/main/resources/db/migration/V<next>__create_notifications.sql
backend/src/main/resources/db/dev-data/R__demo_notifications.sql

frontend/src/features/notifications/{api,components,pages?}
frontend/src/app/{client-area,trainer-area}.tsx                  # mount the bell
frontend/src/lib/{api/schema.d.ts,query-keys.ts}
frontend/src/locales/{en,pl}.json
```

## Phases

- **#151** model, migration, list and mark-read API, security rule, tests, demo data, regenerated types.
- **#152** `NoticeBoard`, the recorder, the eleven raising sites with tests.
- **#153** event `notification` on the stream.
- **#154** bell, side sheet, toast, routes, strings, tests.
- **#155** Approvals count refreshed on the event.
- **#296** the deletion notice and the erasure test.

One pull request for the story, one commit per task, each ending in `(#<task>)`.
