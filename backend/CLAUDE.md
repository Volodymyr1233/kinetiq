# Kinetiq backend

REST API. Java 25, Spring Boot 4.1 (Spring MVC, virtual threads), Spring Security 7, Spring Data JPA (Hibernate 7), Flyway, PostgreSQL 18, MapStruct, Lombok, springdoc (development only), JUnit 5, AssertJ, Testcontainers, ArchUnit. Gradle Kotlin DSL.

## Architecture: hexagonal, by feature

```
com.kinetiq
├─ common                     shared kernel
│  ├─ domain                  value objects and exceptions used by several features
│  └─ infrastructure
│     └─ config               SecurityConfig, TransactionConfig
└─ <feature>                  e.g. plan, session, readiness, messaging
   ├─ domain
   │  ├─ model                aggregates, entities, value objects, typed ids
   │  ├─ input                driving ports: use case interfaces and their commands
   │  ├─ output               driven ports: repositories, gateways
   │  ├─ service              use case implementations
   │  └─ exception            domain exceptions
   └─ infrastructure
      ├─ config               @Configuration that wires this feature's services
      ├─ input                driving adapters: REST controllers, request/response records, web mappers
      └─ output               driven adapters: persistence, LLM, mail
```

Rules, enforced by `HexagonalArchitectureTest`:

- `domain` depends on nothing but the JDK, Lombok and SLF4J. No Spring, Jakarta, Hibernate, Jackson or MapStruct.
- `domain` never depends on `infrastructure`.
- `infrastructure.input` and `infrastructure.output` never depend on each other. A controller reaches data only through a use case.
- A feature's `infrastructure` is used only inside that feature. Other features call its `domain.input` ports.
- Features have no dependency cycles. `common` depends on no feature.

When a new structural rule is agreed, add it to that test.

## Naming

| Thing | Convention | Example |
|---|---|---|
| Use case port | `<Verb><Noun>UseCase`, interface in `domain.input`; may group closely related operations | `ManagePlanUseCase` |
| Command | record in `domain.input`, `<Verb><Noun>Command` | `UpdatePlanCommand` |
| Use case implementation | `<Noun><Activity>Service` in `domain.service`, no Spring annotations | `PlanManagementService` |
| Repository port | `<Aggregate>Repository` in `domain.output` | `PlanRepository` |
| Gateway port | role noun in `domain.output` | `PlanGenerator`, `NotificationSender` |
| Persistence adapter | `<Port>Impl`, `@Repository` | `PlanRepositoryImpl` |
| Spring Data repository | `Jpa<Aggregate>Repository` | `JpaPlanRepository` |
| Other output adapter | technology prefix + port noun | `AiPlanGenerator`, `LoggingNotificationSender` |
| JPA entity | `<Domain>Entity`, table in plural snake_case | `PlanEntity` → `plans` |
| Typed id | `<Aggregate>Id` record | `PlanId` |
| Controller | `<Aggregate>Controller`, `@RestController` | `PlanController` |
| Request / response | records, `<Action><Noun>Request`, `<Noun>Response` | `UpdatePlanRequest`, `PlanResponse` |
| Mapper | MapStruct; `<Aggregate>Mapper` (persistence), `<Aggregate>WebMapper` (web) | `PlanMapper` |

Names come from `CONTEXT.md` at the repository root.

## Java

Use the current language and its idioms.

- Records for ids, value objects, commands, requests, responses and other data carriers.
- Immutable by default: `final` fields, unmodifiable collections (`List.of`, `Map.copyOf`, `Stream.toList()`), defensive copies in record constructors. Only aggregates change state, through their methods.
- `Optional` for a result that may be absent, never `null`. Not for fields or parameters.
- Streams for transforming collections; a loop when it reads better.
- Pattern matching for `instanceof` and `switch`, switch expressions, text blocks, `var` when the type is plain from the right-hand side.
- Virtual threads are on (`spring.threads.virtual.enabled`): write plain blocking code. No reactive types, and no sized pools for blocking work: use `Executors.newVirtualThreadPerTaskExecutor()`.

## Domain

- Use cases take typed ids and command records and return domain objects. Never pass request DTOs or JPA entities into the domain.
- Ids and value objects are records that validate themselves in the compact constructor. Ids wrap a `UUID` and are created in the application (`PlanId.random()`), not by the database.
- Aggregates expose behaviour, not setters: `@Getter` plus methods that keep invariants. No `@Data` on domain classes.
- Finder ports return `Optional`; the service turns an empty result into a domain exception.
- Domain exceptions are unchecked, carry a stable error code, and are not declared in `throws` clauses.
- Timestamps are `Instant`.
- Constructor injection everywhere (`@RequiredArgsConstructor`). Logging through `@Slf4j`, parameterised messages, no personal data in logs.

## Wiring and transactions

- Domain services are plain classes. Register them as beans in the feature's `infrastructure.config` class.
- Never put `@Transactional` or `@Service` on a domain class. `TransactionConfig` wraps every bean that implements a `*UseCase` interface from a `domain.input` package in a transaction. One use case call is one transaction.

## Persistence

- JPA entities live in `infrastructure.output` and never leave it. They use a plain `@Id UUID`; the mapper converts to and from typed ids.
- Entities extend a shared base with `createdAt`, `updatedAt` and `@Version` once the first entity needs it.
- Enums are stored as strings. Associations are lazy; fetch what a use case needs with an entity graph or a query.
- Schema changes are Flyway migrations in `src/main/resources/db/migration`, `V<n>__snake_case.sql`, written by hand with named constraints. Never edit a migration that has been merged. Hibernate only validates (`ddl-auto: validate`).
- Demo data goes in `src/main/resources/db/dev-data` as repeatable migrations (`R__*.sql`, idempotent inserts). Only the `local` profile adds that location, so tests and production never run it. Never put real credentials or real personal data in SQL.

## Web

- Endpoints live under `/api/v1`. Controllers are thin: validate, map to a command, call one use case, map the result.
- Request bodies are records with Bean Validation annotations and `@Valid`.
- Never return domain objects or entities from a controller.
- Errors are RFC 9457 problem details (`application/problem+json`) with an extra `code` property, e.g. `plan.not-found`. Validation failures add `errors: [{field, code}]`. Map domain exceptions in one `@RestControllerAdvice` in `common.infrastructure.input`, extending `ResponseEntityExceptionHandler`.
- `detail` holds only text written for that purpose. Unknown exceptions become `500` with code `internal.error`, a generic message and a correlation id; the exception itself is only logged.
- springdoc serves `/v3/api-docs` and `/swagger-ui.html` in development. It is a `developmentOnly` dependency and is absent from the production jar. After changing the API, regenerate the frontend types in the same pull request.

## Security

- `SecurityConfig` denies everything by default. Open an endpoint only when the spec says it is public.
- Session cookie authentication with SPA CSRF (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header). No JWT.
- Check ownership in the use case: a trainer acts only on their own clients, a client only on their own data. Answer `404` for another person's resource.
- Login failures give the same response whether the account exists or not.

## LLM use

- An LLM is reached only through a gateway port; the domain decides what is acceptable.
- Rules over logged data decide when a plan change is due. An LLM may draft its content; the domain validates the draft against bounds before it is stored.
- Today-only adjustments are computed by rules, never by an LLM.
- Prompts never contain names, emails or ids of a person.

## Tests

- Naming: `shouldX_whenY`. Group by tested method with `@Nested` when a class tests more than one. Assertions with AssertJ.
- Test behaviour: business rules and important edge cases. Every test has a clear purpose and fails when the behaviour it covers breaks.
- Do not write tests that only call getters, setters, constructors or trivial delegation, or that pin implementation details. No tests written to raise coverage.
- Tests are deterministic, independent, readable. Aim for high coverage of important logic, not a percentage.

| Subject | Kind | Name |
|---|---|---|
| Domain service, aggregate, value object | plain unit test; hand-written fakes for output ports, no Spring | `*Test` |
| Controller | `@WebMvcTest` with the use case mocked; covers status codes, validation, error mapping, authorisation | `*Test` |
| Persistence adapter | `@DataJpaTest` with Testcontainers PostgreSQL (`TestcontainersConfiguration`) | `*IT` |
| Whole application | one `@SpringBootTest` smoke test | `KinetiqBackendApplicationIT` |

Fakes for output ports live in test sources next to the tests that use them; move one to a shared test package when a second feature needs it.

## Commands

Run in `backend/`. Docker must be running for both.

- `./gradlew bootRun`: starts the API on `:8080` with the `local` profile, and PostgreSQL from the root `compose.yaml` (port `15432`, database, user and password `kinetiq`).
- `./gradlew check`: compiles and runs every test, including the architecture rules. Must pass before a commit.
- `./gradlew test --tests '<pattern>'`: a subset.

When running from the IDE, keep the working directory at the repository root so `compose.yaml` is found, and set the active profile to `local`.
