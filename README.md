# Kinetiq

Training system for trainers and their clients. Spring Boot REST API in `backend/`, React single-page app in `frontend/`.

## Prerequisites

- JDK 25 (`JAVA_HOME` pointing at it)
- Node.js 24 with npm
- Docker Desktop, running
- [GitHub CLI](https://cli.github.com/), logged in with `gh auth login`
- A `CONTEXT7_API_KEY` environment variable ([context7.com](https://context7.com)) for the documentation MCP server used by Claude Code
- Optionally `OPENROUTER_API_KEY` and `OPENROUTER_MODEL` environment variables (see `.env.example`) for drafting first plans; without a key, every quiz submit gets an empty failed draft

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
| `demo.client@example.com` | Client, email confirmed, quiz not submitted |
| `demo.client.waiting@example.com` | Client, email confirmed, quiz submitted (waiting for a plan) |
| `demo.client.draft@example.com` | Client, email confirmed, quiz submitted, plan in draft saved by the trainer 3 days ago (waiting for a plan) |
| `demo.client.published@example.com` | Client, email confirmed, quiz submitted, plan published and not opened yet (the plan-ready screen shows) |
| `demo.client.seen@example.com` | Client, email confirmed, quiz submitted, plan published and already opened, week 3 of 8 |
| `demo.client.finished@example.com` | Client, email confirmed, quiz submitted, plan published and finished after week 4 |
| `demo.client.daily@example.com` | Client, email confirmed, quiz submitted, plan published and opened, a session Monday to Saturday (week 3 of 8), check-ins on the three days before the first run (one per readiness band) and none today |
| `demo.client.built@example.com` | Client, email confirmed, quiz submitted, plan drafted automatically 3 hours ago and not reviewed yet |
| `demo.client.failed@example.com` | Client, email confirmed, quiz submitted, automatic drafting failed 26 hours ago: an empty draft |
| `demo.trainer2@example.com` | Trainer, email confirmed, second trainer for isolation checks; 9 of 10 automatic builds of the last 24 hours used |
| `demo.client.other@example.com` | Client of the second trainer, quiz submitted, plan drafted automatically (never in the first trainer's approvals) |
| `demo.client.rebuilding@example.com` | Client, email confirmed, quiz submitted, automatic draft being built again (the sweep ends it a few minutes after start) |

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

## Deploy

Production runs as one Docker image on [Railway](https://railway.com): the backend serves the built SPA, so `/` and `/api` share one origin ([ADR 0001](docs/adr/0001-spa-served-by-backend.md)). The `Dockerfile` in the repository root builds both sides, and Railway needs no build or start command.

1. Create a Railway project with **Deploy from GitHub repo** and pick this repository. Railway finds the `Dockerfile`. Under **Settings > Source**, set the branch to `master`.
2. In the same project, add a **PostgreSQL** database and keep its default name, `Postgres`. The references in the next step use that name. Flyway creates the schema on the first start.
3. Open the app service and add the variables below under **Variables > Raw Editor**.

   ```
   PORT=8080
   SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
   SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
   SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
   SERVER_FORWARD_HEADERS_STRATEGY=framework
   ```

   `PORT` sends Railway's traffic to the port Spring listens on. Railway's own `DATABASE_URL` is not a JDBC URL, hence the three `SPRING_DATASOURCE_*` references. Railway ends TLS before the request reaches the app; without `SERVER_FORWARD_HEADERS_STRATEGY` the app takes the request for plain HTTP and sends the `XSRF-TOKEN` cookie without the `Secure` flag.

   Add the application's own variables from [`.env.example`](.env.example) too, each with its comment. Set `KINETIQ_APP_URL` after step 4, and `MAIL_FROM` to an address on a domain you verified in Resend: Resend delivers from its test address only to the account owner. Never set the `local` profile; it turns on demo accounts and non-Secure cookies.
4. Under **Settings > Networking**, choose **Generate Domain** with port `8080`. Put the resulting `https://` address into `KINETIQ_APP_URL` so links in emails point to it, then redeploy.
5. Under **Settings > Deploy**, set the healthcheck path to `/actuator/health`.

Check the result, with `URL` set to your address:

```bash
curl -s $URL/actuator/health
curl -si $URL/api/v1/csrf | grep -i set-cookie
curl -sI $URL/swagger-ui.html | head -1
```

The first returns `{"status":"UP"}`. The second shows `XSRF-TOKEN` with `Secure`; after signing in, `JSESSIONID` has it too. The third returns `404`, because the API documentation is not in the production jar. Then sign up in the browser and check that the confirmation email arrives and its link opens on your address.

## Where things are written down

| File | What |
|---|---|
| [`CLAUDE.md`](CLAUDE.md) | Workflow, commit rules, architecture overview |
| [`backend/CLAUDE.md`](backend/CLAUDE.md) | Backend architecture and conventions |
| [`frontend/CLAUDE.md`](frontend/CLAUDE.md) | Frontend structure and conventions |
| [`CONTEXT.md`](CONTEXT.md) | Domain vocabulary |
| [`docs/adr/`](docs/adr) | Architecture decision records |
| [`.specify/memory/constitution.md`](.specify/memory/constitution.md) | Project principles |
