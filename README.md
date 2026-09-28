<p align="center">
  <img src="https://img.shields.io/badge/Cluverse-Backend-4f46e5?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/Microservices-Architecture-0ea5e9?style=for-the-badge" />
  <img src="https://img.shields.io/badge/Status-Active-22c55e?style=for-the-badge" />
</p>

<h1 align="center">Cluverse — Backend</h1>
<p align="center"><em>AI-powered Student Club Management SaaS Platform — built by HexaTeam @ ESPRIT</em></p>

---

##  Overview

**Cluverse** is a full-stack, AI-powered SaaS platform designed to unify and digitize student club operations at scale. The backend is built on a **microservices architecture** using Spring Boot, orchestrated with Kubernetes, and enriched with AI capabilities.

This repository hosts all backend services powering Cluverse's core modules.

---

##  Architecture

```
cluverse-backend/
├── api-gateway/              # Central entry point — routing, auth filtering
├── service-registry/         # Eureka discovery server
├── config-server/            # Centralized config management
├── finance-service/          # Budget tracking, Stripe payments, cashflow forecasting
├── events-service/           # Event creation, scheduling, attendance
├── recruitment-service/      # Applications, interview simulator, CV analysis
├── sponsoring-service/       # Sponsor tracking and partnership management
├── elections-service/        # Digital elections with audit trails
├── skills-service/           # Member skill tracking and development
├── logistics-service/        # Resource and logistics coordination
├── ai-service/               # AI features: alerts, CV parsing, recommendations
└── notification-service/     # Smart alerts and email/push notifications
```

---

##  Tech Stack

| Layer              | Technology                                      |
|--------------------|-------------------------------------------------|
| Language           | Java 17                                         |
| Framework          | Spring Boot 3.x                                 |
| Service Discovery  | Netflix Eureka                                  |
| API Gateway        | Spring Cloud Gateway                            |
| Messaging          | RabbitMQ / Kafka                                |
| Database           | MySQL (per-service), Redis (caching)            |
| Authentication     | Keycloak / Spring Security + JWT                |
| Payments           | Stripe (Webhook integration)                    |
| AI / ML            | Python microservice (FastAPI) + REST bridge     |
| Containerization   | Docker                                          |
| Orchestration      | Kubernetes                                      |
| IaC                | Ansible, Terraform                              |
| CI/CD              | GitHub Actions                                  |
| Monitoring         | Prometheus + Grafana                            |

---

##  Modules

| Module         | Description                                                                 |
|----------------|-----------------------------------------------------------------------------|
| **Finance**    | Budget management, expense tracking, Stripe payments, cashflow forecasting  |
| **Events**     | Event lifecycle management, scheduling, attendance, QR check-in             |
| **Recruitment**| Member applications, AI interview simulator, smart CV analysis              |
| **Sponsoring** | Sponsor CRM, partnership pipeline, document generation                      |
| **Elections**  | Transparent digital voting with audit trails and anti-fraud mechanisms      |
| **Skills**     | Member competency tracking, skill gap analysis, development plans           |
| **Logistics**  | Resource booking, inventory management, operational coordination            |
| **AI Service** | Smart alerts, member insights, recommendations, CV parsing                  |

---

##  Getting Started

### Prerequisites

- Java 17+
- Docker & Docker Compose
- Maven 3.9+
- MySQL 8+

### Run with Docker Compose

```bash
git clone https://github.com/the-amine-mokhtar/Backend-CluVerse.git
cd cluverse-backend
docker-compose up --build
```

### Run a single service locally

```bash
cd finance-service
mvn spring-boot:run
```

### Environment Variables

Each service uses an `application.yml`. Copy `.env.example` to `.env` and fill in:

```env
DB_URL=jdbc:mysql://localhost:3306/cluverse_finance
DB_USERNAME=root
DB_PASSWORD=yourpassword
STRIPE_SECRET_KEY=sk_test_...
JWT_SECRET=your_jwt_secret
EUREKA_SERVER=http://localhost:8761/eureka
```

---

##  Authentication Flow

```
Client → API Gateway → Keycloak (JWT validation) → Target Service
```

All routes are protected by JWT. Role-based access control (RBAC) is enforced per service with roles: `ADMIN`, `PRESIDENT`, `MEMBER`, `TREASURER`, `GUEST`.

---

##  AI Features

The `ai-service` exposes a REST API wrapping Python ML models:

| Feature                    | Description                                         |
|----------------------------|-----------------------------------------------------|
| Interview Simulator        | Generates role-specific questions + instant feedback |
| Smart CV Analysis          | Extracts skills and ranks candidates automatically   |
| Smart Alerts               | Anomaly detection on budgets, attendance, and tasks  |
| Member Tracking            | Engagement scoring and retention risk prediction     |
| Cashflow Forecasting       | Time-series prediction for club finances             |

---

##  API Gateway Routes

| Prefix             | Target Service         |
|--------------------|------------------------|
| `/api/finance/**`  | finance-service        |
| `/api/events/**`   | events-service         |
| `/api/recruit/**`  | recruitment-service    |
| `/api/elections/**`| elections-service      |
| `/api/skills/**`   | skills-service         |
| `/api/ai/**`       | ai-service             |
| `/api/notify/**`   | notification-service   |

---

##  Kubernetes Deployment

```bash
kubectl apply -f k8s/
```

Each service has its own `Deployment`, `Service`, and `ConfigMap` under `k8s/<service-name>/`.

---

##  Testing

```bash
mvn test                    # Unit tests
mvn verify                  # Integration tests
```

Test coverage target: **80%+** per service.

---

##  Academic Context

Capstone (PI) project — **ESPRIT School of Engineering**, Tunisia, 2026.

---

<p align="center">Built with Java, Docker, Kubernetes & a lot of efforts</p>
