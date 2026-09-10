<div align="center">

# Subscription Manager

Track every subscription you pay for, see where your money actually goes, and never get blindsided by a renewal again.

![Next.js](https://img.shields.io/badge/Next.js-16-black?logo=next.js&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4-6DB33F?logo=springboot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.4-4479A1?logo=mysql&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-5-3178C6?logo=typescript&logoColor=white)
![Status](https://img.shields.io/badge/status-in%20development-orange)

</div>

---

## About

Subscription Manager is a full-stack web app for keeping track of recurring
subscriptions — streaming services, SaaS tools, memberships, anything you're
billed for on a schedule. It normalizes costs across billing cycles, tracks
payment and price history, and surfaces spending trends so you always know
what you're paying for and why.

Built as a portfolio-grade full-stack project: a Next.js frontend, a Spring
Boot REST API, and a MySQL database, all containerized with Docker.

## Features

- **Subscription tracking** — create, edit, and categorize subscriptions with custom or default categories, billing cycles, and status
- **Payment history** — log payments per subscription and review transaction history
- **Cost normalization** — see monthly vs. yearly spend on equal footing across every subscription
- **Dashboard & analytics** — spending trends, category breakdowns, upcoming renewals, and top expenses at a glance
- **Price change tracking** — historical price snapshots with percentage-change indicators
- **Renewal & trial reminders** — in-app notifications before a subscription renews or a free trial ends
- **Budgeting** — set a monthly budget and get warned when you're about to exceed it
- **"What if I cancel?" calculator** — multi-select subscriptions to see potential savings
- **Usage insights** — flag rarely-used subscriptions that are quietly draining your budget
- **CSV / PDF export** — take your data with you
- **JWT authentication** — short-lived access tokens with refresh-token rotation, plus password reset

> Some of the above are still in progress — see [Roadmap](#roadmap).

## Tech Stack

| Layer          | Technology                                                        |
| -------------- | ------------------------------------------------------------------ |
| Frontend       | Next.js 16 (App Router), TypeScript, Tailwind CSS                  |
| Backend        | Spring Boot 4 — Web, Data JPA, Security, Validation, Actuator      |
| Database       | MySQL 8.4                                                          |
| Auth           | JWT (access + refresh tokens), BCrypt                              |
| Infrastructure | Docker, Docker Compose                                             |

## Getting Started

### Prerequisites

- [Docker](https://www.docker.com/) and Docker Compose
- Node.js 20+ and Java 17+ (only needed for running services outside Docker)

### Run with Docker (recommended)

```bash
git clone https://github.com/mehedipriam/subscription-manager.git
cd subscription-manager
cp .env.example .env
docker compose up --build
```

| Service            | URL                                   |
| ------------------- | -------------------------------------- |
| Frontend             | http://localhost:3000                 |
| Backend API           | http://localhost:8080/api/v1          |
| Backend health check | http://localhost:8080/api/v1/health   |
| Actuator health       | http://localhost:8080/actuator/health |

> If those ports are already in use on your machine, override `FRONTEND_PORT`, `BACKEND_PORT`, and `DB_PORT` in `.env`.

### Run locally without Docker

**Backend**

```bash
cd backend
./mvnw spring-boot:run
```

Needs a running MySQL instance — point the app at it with the `DB_HOST`,
`DB_PORT`, `DB_NAME`, `DB_USERNAME`, and `DB_PASSWORD` environment variables
(defaults live in `.env.example`).

**Frontend**

```bash
cd frontend
npm install
npm run dev
```

## Environment Variables

All variables are documented with sensible local defaults in `.env.example`.

| Variable               | Description                                    | Default                  |
| ----------------------- | ----------------------------------------------- | ------------------------- |
| `DB_NAME`                | MySQL database name                             | `subscription_manager`     |
| `DB_USERNAME` / `DB_PASSWORD` | MySQL credentials                          | `subscription_manager`     |
| `DB_PORT`                | Host port MySQL is exposed on                   | `3306`                     |
| `BACKEND_PORT`           | Host port the backend is exposed on             | `8080`                     |
| `FRONTEND_PORT`          | Host port the frontend is exposed on            | `3000`                     |
| `FRONTEND_ORIGIN`        | Allowed CORS origin for the backend             | `http://localhost:3000`    |
| `NEXT_PUBLIC_API_URL`    | Backend base URL the browser calls              | `http://localhost:8080`    |
| `JWT_SECRET`              | Signing key for access tokens (32+ bytes)       | *(generate your own)*      |
| `JWT_ACCESS_TTL_MINUTES`  | Access token lifetime                           | `15`                        |
| `JWT_REFRESH_TTL_DAYS`    | Refresh token lifetime                          | `7`                         |
| `PASSWORD_RESET_TTL_MINUTES` | Password reset token lifetime               | `30`                        |
| `RATE_LIMIT_MAX_REQUESTS` | Max requests per window on auth endpoints       | `10`                        |
| `RATE_LIMIT_WINDOW_SECONDS` | Rate limit window length                      | `60`                        |

## Project Structure

```
subscription-manager/
├── frontend/          # Next.js app (App Router, TypeScript, Tailwind)
├── backend/           # Spring Boot REST API
├── docker-compose.yml # MySQL + backend + frontend orchestration
└── .env.example       # Environment variable reference
```

## Roadmap

- [x] Project scaffolding, Docker orchestration, and service health checks
- [x] Database schema & JPA entities
- [x] Authentication (JWT, refresh tokens, password reset, rate limiting)
- [x] Subscription CRUD & categories
- [x] Payment history
- [x] Cost normalization & dashboard
- [x] Analytics (spend trends, category breakdown)
- [x] Price history & change tracking
- [x] Notifications & scheduled reminders
- [x] Budgeting & savings calculator
- [x] Usage tracking & recommendations
- [ ] Responsive UI, dark mode, CSV/PDF export
- [ ] Automated testing (JUnit/Mockito, frontend tests)

## Author

[@mehedipriam](https://github.com/mehedipriam)
