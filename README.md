# MES Product Workspace

This repository is the product development workspace for the electric fan manufacturing MES.

## Technology Stack

- Web frontend: Vue 3 + Vue Router 4
- Mini Program: native WeChat Mini Program
- Backend: Spring Boot + RESTful API
- Database: MySQL 8.0+
- Migration convention: Flyway-compatible SQL files under `backend/mes-server/src/main/resources/db/migration`
- Security: symmetric encryption implemented on both frontend and backend

## Project Layout

```text
MES/
  backend/
    mes-server/                    Spring Boot backend service
      src/main/java/com/yunshu/mes
      src/main/resources/db/migration
      src/main/resources/db/seed
      src/test/java/com/yunshu/mes
  frontend/
    web-admin/                     Vue 3 management frontend
    wechat-miniprogram/            Native WeChat Mini Program
    shared/                        Frontend shared contracts and utilities
  database/
    migrations/                    Standalone database initialization SQL
    seeds/                         Standalone development seed data
    docs/                          Database implementation notes
  docs/
    architecture/                  Architecture documents
    api/                           RESTful API documents
    database/                      Database design and usage documents
    deployment/                    Deployment documents
    security/                      Security and encryption documents
  ops/
    docker/                        Docker and local infrastructure
    scripts/                       Cross-platform scripts
    ci/                            CI configuration
  security/
    crypto-spec/                   Symmetric encryption protocol/spec
    test-vectors/                  Cross-platform encryption test vectors
  shared/
    contracts/                     Shared API contract files
    schemas/                       Shared JSON/OpenAPI/schema files
  tools/                           Development tooling
```

## Database Files

Standalone SQL files:

```text
database/migrations/001_core_schema.sql
database/migrations/002_mes_business_schema.sql
database/migrations/003_agent_schema.sql
database/seeds/001_seed_basic_data.sql
```

Spring Boot/Flyway migration files:

```text
backend/mes-server/src/main/resources/db/migration/V1__core_schema.sql
backend/mes-server/src/main/resources/db/migration/V2__mes_business_schema.sql
backend/mes-server/src/main/resources/db/migration/V3__agent_schema.sql
backend/mes-server/src/main/resources/db/seed/R__seed_basic_data.sql
```

## Local Database Initialization

Standalone execution:

```bash
mysql -u root -p < database/migrations/001_core_schema.sql
mysql -u root -p < database/migrations/002_mes_business_schema.sql
mysql -u root -p < database/migrations/003_agent_schema.sql
mysql -u root -p < database/seeds/001_seed_basic_data.sql
```

Flyway execution should use a connection whose default schema/database is `fan_mes`.

## Current Scaffold Status

The repository already includes:

- Spring Boot backend with unified `ApiResponse`, `traceId`, and mock APIs for `health`, `dashboard`, and `system`
- Vue 3 web admin with module routes, sidebar menu, and scaffold pages for all business domains
- MySQL schema and seed SQL in both standalone and Flyway locations
- `system` as the first end-to-end sample: mock API data aligned with seed SQL and consumed by the system page with frontend fallback

Default local development does **not** require MySQL. The `dev` profile serves mock data so the frontend can connect immediately.

## Quick Start

Prerequisites:

- JDK 17
- Maven 3.9+
- Node.js 20+

### 1. Verify the workspace

```bash
./ops/scripts/verify-local.sh
```

Windows PowerShell:

```powershell
.\ops\scripts\verify-local.ps1
```

The script checks SQL dual-track sync, backend tests, frontend build, module directories, and legacy naming.

### 2. Start backend mock API

```bash
cd backend/mes-server
mvn spring-boot:run
```

Backend default URL:

```text
http://127.0.0.1:8080/api/health
```

### 3. Start web admin

```bash
cd frontend/web-admin
npm install
npm run dev
```

Default local pages:

```text
http://127.0.0.1:5173/login
http://127.0.0.1:5173/dashboard/workbench
http://127.0.0.1:5173/system/users
```

The Vite dev server proxies `/api` to `http://127.0.0.1:8080`.

### 4. Optional: backend with MySQL and Flyway

```bash
cd backend/mes-server
DB_URL='jdbc:mysql://localhost:3306/fan_mes?createDatabaseIfNotExist=true&serverTimezone=Asia/Shanghai' \
DB_USERNAME=root \
DB_PASSWORD=your_password \
FLYWAY_ENABLED=true \
SPRING_PROFILES_ACTIVE=dev \
mvn spring-boot:run
```

## Team Collaboration

Read these files before assigning work:

- `CONTRIBUTING.md`: branch, commit, merge, and conflict rules.
- `docs/collaboration/team_split.md`: suggested four-person ownership split.
- `docs/collaboration/module_boundaries.md`: backend/frontend module boundaries.
- `docs/collaboration/merge_workflow.md`: daily development and merge workflow.
- `docs/collaboration/review_checklist.md`: checklist before sending code to the project lead.

Run the full local verification before merging:

```bash
./ops/scripts/verify-local.sh
```

Windows PowerShell:

```powershell
.\ops\scripts\verify-local.ps1
```

## Cross-platform Rules

- All project paths use English names.
- Text files should use UTF-8.
- Scripts should avoid OS-specific path separators where possible.
- Do not rely on symlinks for core project structure.
- Keep generated artifacts outside source directories unless explicitly needed.
