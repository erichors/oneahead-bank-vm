# OneAhead Bank AI Context

This file is a reusable context packet for humans or AI coding agents that need to understand, deploy, fork, or extend OneAhead Bank VM Edition.

## Important Clarification

OneAhead Bank does not currently run an AI model at application runtime. There is no LLM, model server, inference endpoint, prompt pipeline, vector database, or AI SDK in the deployed app.

In this repo, "AI context" means documentation for using an AI coding assistant to work on the project elsewhere. It explains the product intent, architecture, conventions, extension points, and safe next changes.

## Product Summary

OneAhead Bank is a small multi-tier banking demo intended for VM and EC2 observability scenarios. It has a polished React banking UI, a Java backend with an embedded database, and a separate Java credit scoring service.

The app is intentionally simple enough to deploy on three VMs while still producing useful service-to-service traffic and controllable problem patterns.

## Target Architecture

```text
Browser
  -> Frontend VM: React app on port 8081
  -> Backend VM: Java Spring Boot API on port 8082
       -> H2 file database under backend/data/
       -> Credit VM: Java Spring Boot credit service on port 8084
```

The intended deployment uses three VMs:

- Frontend VM: serves the React UI and admin console.
- Backend VM: runs the banking API and owns the database.
- Credit VM: runs the credit scoring API.

The database is intentionally colocated with the backend tier, so this remains a 3-tier VM architecture. If the database is moved to its own VM later, the app becomes a 4-tier architecture.

## Repository Layout

```text
.
|-- README.md
|-- docs/
|   `-- AI_CONTEXT.md
|-- pom.xml
|-- backend/
|   |-- pom.xml
|   `-- src/main/java/com/banking/
|-- credit-service/
|   |-- pom.xml
|   `-- src/main/java/com/banking/credit/
|-- frontend/
|   |-- package.json
|   |-- public/
|   `-- src/
`-- scripts/
    |-- drive-traffic.sh
    |-- run-backend.sh
    |-- run-credit-service.sh
    `-- run-frontend.sh
```

## Runtime Services

### Frontend

- Path: `frontend/`
- Framework: React 18 with Create React App
- Local/dev port: `8081`
- Important env var: `REACT_APP_API_URL`
- Production build command:

```bash
cd frontend
REACT_APP_API_URL=http://BACKEND_VM_HOST:8082 npm run build
```

The frontend contains normal banking screens and an Admin page. The Admin page can generate browser-side load and toggle backend problem patterns.
The visible navigation uses a settings gear for the operational controls instead of an "Admin" navigation label.

### Backend

- Path: `backend/`
- Framework: Java 17, Spring Boot 3.2
- Port: `8082`
- Database: H2 file database
- Database location: `backend/data/`
- Important env var: `CREDIT_SERVICE_URL`

Primary API groups:

- `/api/account/*`: balance, deposit, transfer
- `/api/demo/*`: seeded demo users, login, and account summary data
- `/api/credit/check`: backend proxy into the credit tier
- `/api/admin/*`: problem-pattern controls and admin config
- `/actuator/health`: Spring Boot health

### Credit Service

- Path: `credit-service/`
- Framework: Java 17, Spring Boot 3.2
- Port: `8084`

Primary API groups:

- `/api/credit/check`: deterministic credit score simulation
- `/api/credit/health`: lightweight service health
- `/actuator/health`: Spring Boot health

## Configuration Model

VM service discovery is environment-variable based:

```bash
CREDIT_SERVICE_URL=http://CREDIT_VM_HOST:8084/api/credit/check
REACT_APP_API_URL=http://BACKEND_VM_HOST:8082
```

The repo scripts also accept host-level variables:

```bash
CREDIT_HOST=54.175.159.199 ./scripts/run-backend.sh
BACKEND_HOST=34.238.42.210 ./scripts/run-frontend.sh
```

Demo users:

```text
tbrady / goat   Thomas Brady, Miami, Florida
dmorgan / ahead1 Dave Morgan, Naperville, Illinois
mlowe / ahead1  Matt Lowe, Cleveland, Ohio
dshah / ahead1  Dipen Shah, Edison, New Jersey
```

Do not reintroduce Kubernetes service names like `backend-service` or `credit-service` unless creating a separate Kubernetes variant.

## Built-In Problem Patterns

Problem settings are stored in the backend database through `AdminConfig`.

Current keys:

```text
error.404.enabled
sql.slow.enabled
sql.slow.delay
credit.slow.enabled
credit.slow.delay
problem.cpu.enabled
problem.cpu.millis
```

Current problem behavior:

- `error.404.enabled`: account and credit proxy endpoints return 404.
- `sql.slow.enabled`: deposit and transfer add a configured delay.
- `credit.slow.enabled`: backend waits before calling the credit tier.
- `problem.cpu.enabled`: backend burns CPU for a configured number of milliseconds on account and credit proxy requests.

## Adding More Problem Patterns

Preferred pattern:

1. Add new config keys in `backend/src/main/java/com/banking/service/AdminService.java`.
2. Add a small method that reads the config and applies the problem behavior.
3. Call that method from the narrowest relevant controller or service path.
4. Add a card/toggle in the React Admin page.
5. Document the new keys in this file and in `README.md` if users need to operate them.

Good future examples:

- Backend memory pressure: allocate and retain a configurable amount of memory.
- Credit service errors: return intermittent 500s from the credit tier.
- Credit service latency: add latency inside the credit service instead of before the backend call.
- Database lock contention: hold a transaction open for a configurable duration.
- Noisy logs: emit warning/error logs at a controlled rate.

Avoid destructive patterns by default. Do not add patterns that delete data, exhaust disk permanently, disable SSH, or require root unless explicitly requested.

## Load Generation

There are two load-generation options:

1. Browser load slider behind the settings gear. It defaults to 3 requests per second, persists in browser local storage, and creates debit/credit transactions across seeded users through `/api/demo/transactions/random`.
2. Headless shell driver:

```bash
BACKEND_URL=http://BACKEND_VM_HOST:8082 RATE=10 DURATION_SECONDS=600 ./scripts/drive-traffic.sh
```

Use `CONCURRENT=true` for more aggressive traffic:

```bash
BACKEND_URL=http://BACKEND_VM_HOST:8082 RATE=20 DURATION_SECONDS=600 CONCURRENT=true ./scripts/drive-traffic.sh
```

Each request group sends balance, deposit, transfer, and credit-check traffic.

## Build And Verification

Java services:

```bash
mvn clean package
```

Frontend:

```bash
cd frontend
npm install
npm run build
```

Smoke tests:

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8084/actuator/health
curl http://localhost:8082/api/account/balance
curl -X POST http://localhost:8082/api/credit/check \
  -H 'content-type: application/json' \
  -d '{"ssn":"123-45-6789","metadata":"smoke"}'
```

## Design Direction

The UI should feel like a modern consumer fintech dashboard:

- Brand: OneAhead Bank
- Visual tone: clean, confident, polished
- Current palette: dark navy, saturated blue, cyan accent, muted financial neutrals
- Layout: practical app-first screens, not a marketing landing page
- Admin page: operational control room for load and problem patterns

Keep screens dense enough for repeated demo use. Avoid decorative-only sections that make the app harder to operate.

## Development Rules For Future AI Agents

Use these rules when asking an AI assistant to work on this repo:

- Preserve the 3-VM architecture unless explicitly asked to split the database or add services.
- Keep React for the frontend.
- Keep backend and credit service Java/Spring Boot.
- Prefer environment variables for VM hostnames and ports.
- Do not commit `node_modules`, `frontend/build`, Java `target` directories, or `backend/data`.
- Keep problem patterns controllable from the Admin page.
- Keep problem patterns off by default.
- Run `mvn clean package` and `npm run build` before pushing changes when dependencies are available.

## Reusable AI Prompt

Use this prompt to start work in another AI coding environment:

```text
You are working on OneAhead Bank VM Edition, a public 3-tier VM banking demo.

Architecture:
- Frontend VM: React app on port 8081.
- Backend VM: Java Spring Boot API on port 8082 with embedded H2 database under backend/data/.
- Credit VM: Java Spring Boot credit service on port 8084.

Service discovery is environment-variable based:
- Frontend uses REACT_APP_API_URL to call the backend.
- Backend uses CREDIT_SERVICE_URL to call the credit service.

The app has an Admin page for load generation and problem toggles. Existing problem keys include 404 errors, slow SQL, slow credit, and backend CPU burn.

Preserve the 3-tier VM shape, keep React, keep Java/Spring Boot for backend services, keep problem patterns off by default, and update README.md plus docs/AI_CONTEXT.md when adding operational behavior.

Before finishing, run:
- mvn clean package
- cd frontend && npm run build
```

## Public Repo

Canonical public repo:

```text
https://github.com/erichors/oneahead-bank-vm
```
