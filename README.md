# YardFlow Operations

A Spring Boot operations platform for container repair, porta cabin production, dispatch, inventory, and live delivery tracking.

## Problems it solves

- Replaces manual WhatsApp location sharing with centralized delivery tracking.
- Tracks container repair and porta cabin production stage-by-stage.
- Shows blocked jobs, overdue work, low-stock materials, and delayed deliveries.
- Keeps customer, asset, work-order, dispatch, and audit history in one system.
- Supports live GPS updates with destination geofencing.

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Spring Web / Validation
- Spring Data JPA + Hibernate
- Spring Security scaffold
- Spring WebSocket
- PostgreSQL production profile
- H2 local development profile
- Redis dependency reserved for production live-state caching
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
- GPS Location History
- Geofencing
- Operational Dashboard
- Audit Timeline

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
```

## Run locally

Requirements: Java 21 and Maven.

```bash
mvn spring-boot:run
```

Open:

- Dashboard: `http://localhost:8080`
- H2 Console: `http://localhost:8080/h2-console`
- Health: `http://localhost:8080/actuator/health`

The development profile seeds realistic demo data so the UI is not empty on first start.

## Live delivery tracking

Open a delivery from the dashboard using **Track**. During development, the tracking page includes a GPS simulator that sends coordinates to:

```http
POST /api/deliveries/{deliveryId}/locations
```

When the vehicle enters the configured destination radius, the backend automatically updates the delivery to `NEAR_DESTINATION`.

## Production database

Run with the `prod` profile and configure:

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
```

Example:

```bash
java -jar target/operation-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

## Core API examples

```text
GET    /api/dashboard
GET    /api/customers
POST   /api/customers
GET    /api/assets
POST   /api/assets
GET    /api/work-orders
POST   /api/work-orders
PATCH  /api/work-orders/{id}
POST   /api/work-orders/{id}/updates
GET    /api/inventory
POST   /api/inventory
POST   /api/deliveries
PATCH  /api/deliveries/{id}
POST   /api/deliveries/{id}/locations
GET    /api/deliveries/{id}/locations/latest
GET    /api/timeline/{type}/{id}
```

## Current security note

The current repository intentionally keeps operational APIs open for local development/demo convenience. Before a public deployment, replace the development security policy with JWT/RBAC and restrict roles such as Admin, Operations Manager, Workshop Manager, QC Inspector, Driver, and Customer.

## Next production-hardening milestones

1. JWT authentication and role-based authorization.
2. Redis-backed latest-location state and stale-driver alerts.
3. Real object-storage uploads for before/after repair photos and POD.
4. QR asset labels and public customer tracking tokens.
5. Vehicle/driver master data and smart assignment.
6. Quotations, invoices, complaints, warranty, and purchase requests.
7. Flyway database migrations and deployment configuration.
