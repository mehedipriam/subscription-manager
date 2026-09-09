# Subscription Manager

A full-stack subscription tracking app: Next.js (TypeScript, Tailwind) frontend, Spring Boot backend, MySQL database, all wired together with Docker Compose.

> Work in progress — being built phase by phase. See `prompt/Subscription_Manager_Plan.pdf` for the full build plan.

## Stack

- **Frontend**: Next.js 16 (App Router, TypeScript, Tailwind CSS) — `frontend/`
- **Backend**: Spring Boot 4 (Web, JPA, Security, Validation, Actuator) — `backend/`
- **Database**: MySQL 8.4
- **Orchestration**: Docker Compose

## Getting started

```bash
cp .env.example .env
docker compose up --build
```

- Frontend: http://localhost:3000
- Backend health: http://localhost:8080/api/v1/health
- Backend actuator health: http://localhost:8080/actuator/health

## Local development (without Docker)

**Backend**

```bash
cd backend
./mvnw spring-boot:run
```

Requires a MySQL instance reachable via the `DB_HOST`/`DB_PORT`/`DB_NAME`/`DB_USERNAME`/`DB_PASSWORD` env vars (see `.env.example`).

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

## Status

- [x] Phase 1 — Project setup
- [ ] Phase 2 — Database & entities
- [ ] Phase 3 — Authentication
- [ ] ...

Full phase list in `prompt/Subscription_Manager_Plan.pdf`.
