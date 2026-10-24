# Kinetiq

Training system for trainers and their clients. Spring Boot REST API in `backend/`, React single-page app in `frontend/`.

## Prerequisites

- JDK 25 (`JAVA_HOME` pointing at it)
- Node.js 24 with npm
- Docker Desktop, running
- [GitHub CLI](https://cli.github.com/), logged in with `gh auth login`
- A `CONTEXT7_API_KEY` environment variable ([context7.com](https://context7.com)) for the documentation MCP server used by Claude Code

## Run

```bash
cd backend
./gradlew bootRun
```

Starts the API on http://localhost:8080 with the `local` profile, and PostgreSQL 18 from `compose.yaml` on port 15432 (database, user and password: `kinetiq`). API documentation: http://localhost:8080/swagger-ui.html.

The `local` profile adds demo data. Every account uses the password `Kinetiq-demo-1`:

| Email | State |
|---|---|
| `demo.trainer@example.com` | Trainer, email confirmed |
| `demo.unconfirmed@example.com` | Trainer, email not confirmed |
| `demo.client@example.com` | Client, email confirmed |

```bash
cd frontend
npm install
npm run dev
```

Starts the app on http://localhost:5173. Requests to `/api` are proxied to the backend.

## Check before committing

```bash
cd backend && ./gradlew check
```

```bash
cd frontend && npm run check
```

## IntelliJ IDEA

Open the repository root, not `backend/`. Then open `backend/build.gradle.kts` and choose **Load Gradle Project**. Set the project SDK and the Gradle JVM to JDK 25. In the run configuration of `KinetiqBackendApplication`, set **Active profiles** to `local`, the working directory to the repository root, and clear **Enable launch optimization** (its C1-only JIT makes sign-up take several seconds).

## Where things are written down

| File | What |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | Workflow, commit rules, architecture overview |
| [`backend/CLAUDE.md`](backend/CLAUDE.md) | Backend architecture and conventions |
| [`frontend/CLAUDE.md`](frontend/CLAUDE.md) | Frontend structure and conventions |
| [`CONTEXT.md`](CONTEXT.md) | Domain vocabulary |
| [`docs/adr/`](docs/adr) | Architecture decision records |
| [`.specify/memory/constitution.md`](.specify/memory/constitution.md) | Project principles |
