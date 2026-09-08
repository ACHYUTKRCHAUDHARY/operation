# YardFlow Operations

A Spring Boot operations platform for container repair, porta cabin production, dispatch, inventory, live delivery tracking, fleet, billing, procurement, warranty, and complaints.

## Reliability

Mutation APIs support optional database-backed idempotency using the `Idempotency-Key` request header. Repeating the same JSON `POST`/`PATCH` request with the same key replays the original successful response instead of executing the business operation again. Reusing the same key for a different payload returns `409 Conflict`, while an identical request that is still processing also returns `409` with `Retry-After: 1`.

Example:

```http
POST /api/work-orders
Idempotency-Key: wo-client-2026-00042
Content-Type: application/json
```

The idempotency scope includes HTTP method, request path, authenticated client headers/cookies, and a SHA-256 request fingerprint. Completed results are retained for 24 hours by default (`app.idempotency.ttl-hours`). Multipart file-upload endpoints are intentionally excluded.

## Problems it solves

- Replaces manual WhatsApp location sharing with centralized delivery tracking.
- Tracks container repair and porta cabin production stage-by-stage.
- Shows blocked jobs, overdue work, low-stock materials, and delayed deliveries.
- Keeps customer, asset, work-order, dispatch, commercial, procurement, warranty, and audit history in one system.
- Supports live GPS updates with destination geofencing.

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Spring Web / Validation
- Spring Data JPA + Hibernate
- Spring Security + JWT
- Spring WebSocket
- PostgreSQL + Flyway production profile
- H2 local development profile
- Redis latest-location cache
- Vanilla HTML/CSS/JavaScript
- Leaflet + OpenStreetMap
- Maven + GitHub Actions

## Main modules

- Customers
- Assets: Containers and Porta Cabins
- Work Orders: repair, production, maintenance, installation
- Work Stage Updates
- Inventory and low-stock alerts
- Dispatch and Delivery
- GPS Location History, Redis latest state, stale tracking, and geofencing
- Fleet: Drivers, Vehicles, Assignment
- Quotations, Invoices, Payments
- Suppliers and Purchase Requests
- Warranty and Complaints
- Secure public tracking tokens + QR
- Proof-of-delivery / file uploads
- Operational and management frontends
- Audit Timeline
- Database-backed Idempotency

## Workflow

```text
Customer
  -> Asset
  -> Inspection
  -> Repair / Production
  -> Quality Check
  -> Ready for Dispatch
  -> Vehicle + Driver
  -> In Transit
  -> Near Destination
  -> Delivered
  -> Installed
  -> Invoice / Payment
  -> Warranty / Support
```

## Run locally

Requirements: Java 21 and Maven.

```bash
mvn spring-boot:run
```

Open:

- Login: `http://localhost:8080/login.html`
- Operations dashboard: `http://localhost:8080/`
- Management: `http://localhost:8080/management.html`
- H2 Console: `http://localhost:8080/h2-console`
- Health: `http://localhost:8080/actuator/health`

Development bootstrap credentials:

```text
admin@operation.local
Admin@12345
```

Do not use these credentials in production.

## Authentication

Browser sessions use an HttpOnly SameSite JWT cookie. API clients may call `/api/auth/login` and use the returned token as:

```http
Authorization: Bearer <JWT>
```

Roles include Admin, Operations Manager, Workshop Manager, QC Inspector, Driver, and Customer.

## Live delivery tracking

The system stores location history in the database and can cache the latest location in Redis. When a vehicle enters the configured destination radius, the delivery automatically moves to `NEAR_DESTINATION`.

Customers can use a secure expiring tracking token and QR code without receiving internal dashboard access.

## Production

Run:

```bash
java -jar target/operation-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

Required environment variables:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
BOOTSTRAP_ADMIN_EMAIL
BOOTSTRAP_ADMIN_PASSWORD
```

Recommended:

```text
PUBLIC_BASE_URL
REDIS_URL
TRACKING_REDIS_ENABLED=true
SECURE_COOKIE=true
STORAGE_ROOT
```

Production uses Flyway migrations followed by Hibernate `ddl-auto=validate`. Production intentionally has no default JWT secret or admin password.

## CI

Every push to `main` triggers the GitHub Actions Maven build/test workflow.
