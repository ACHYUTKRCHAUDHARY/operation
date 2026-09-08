# YardFlow Operations

YardFlow is a production-style Spring Boot platform for container repair, porta cabin production, workshop operations, dispatch, live GPS delivery tracking, billing, procurement, warranty, and customer support.

## Business problems solved

- Replaces manual WhatsApp live-location sharing with centralized GPS tracking and customer tracking links.
- Tracks containers and porta cabins from receipt, inspection, repair/production, QC, dispatch, delivery, and installation.
- Records work-stage updates, photo evidence, blocked reasons, delays, and audit history.
- Tracks drivers, vehicles, permits, insurance, availability, and dispatch assignment.
- Handles inventory, suppliers, purchase requests, approvals, and stock receipt.
- Handles quotations, customer approval, invoices, partial/full payments, complaints, and warranty coverage.
- Surfaces operational exceptions such as overdue work, delayed deliveries, low stock, stale location data, and compliance problems.

## Tech stack

- Java 21
- Spring Boot 4.1.1
- Spring Web + Validation
- Spring Data JPA / Hibernate
- Spring Security + JWT
- HttpOnly JWT browser authentication + Bearer token API authentication
- Spring WebSocket
- Redis latest-location cache with in-memory development fallback
- PostgreSQL production database
- H2 local development database
- Flyway migrations
- ZXing QR generation
- Multipart file/POD uploads
- Vanilla HTML/CSS/JavaScript
- Leaflet + OpenStreetMap
- Maven + GitHub Actions

## Main modules

### Core operations
- Customers
- Container and porta-cabin assets
- Repair / production / installation work orders
- Stage-by-stage work updates
- Photo evidence
- Quality check and ready-for-dispatch flow
- Audit timeline
- Operational dashboard and exception alerts

### Live delivery
- Delivery creation and dispatch
- Driver/vehicle assignment
- GPS history
- Redis latest-location state
- WebSocket location updates
- Destination geofencing
- Stale-location health checks
- Public opaque tracking tokens
- QR customer tracking links
- Proof-of-delivery uploads

### Fleet
- Drivers and license expiry
- Vehicles
- Insurance expiry
- Permit expiry
- Service due date
- Availability validation before assignment

### Commercial
- Quotations
- Server-side quote total calculation
- Customer approval
- Invoices
- Partial and full payments

### Procurement
- Suppliers
- Purchase requests
- Approval/order/receipt workflow
- Automatic inventory increase when material is received

### Support
- Asset warranties
- Customer complaints
- Automatic warranty-covered detection
- Complaint assignment and resolution

## End-to-end workflow

```text
Customer
  -> Asset received
  -> Inspection
  -> Estimate / quotation
  -> Customer approval
  -> Repair / production
  -> Work-stage updates + photos
  -> Quality check
  -> Ready for dispatch
  -> Driver + vehicle assignment
  -> Dispatch
  -> Live GPS tracking
  -> Near-destination geofence
  -> Proof of delivery
  -> Installation
  -> Invoice / payment
  -> Warranty / complaint support
```

## Run locally

Requirements:

- Java 21
- Maven

```bash
mvn spring-boot:run
```

Local URLs:

- Login: `http://localhost:8080/login.html`
- Business management UI: `http://localhost:8080/management.html`
- Core operations dashboard: `http://localhost:8080/`
- H2 console: `http://localhost:8080/h2-console`
- Health: `http://localhost:8080/actuator/health`

### Development login

The local development defaults are:

```text
Email: admin@operation.local
Password: Admin@12345
```

These values are for local development only. The production profile requires explicit environment variables and does not fall back to the development credentials.

## Production configuration

Run with:

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

Recommended production variables:

```text
PUBLIC_BASE_URL=https://your-domain.example
SECURE_COOKIE=true
REDIS_URL=redis://...
TRACKING_REDIS_ENABLED=true
STORAGE_ROOT=/persistent/path/uploads
```

`JWT_SECRET` should be a strong secret of at least 32 characters. Do not commit production credentials to the repository.

## Authentication and roles

Supported roles:

```text
ADMIN
OPERATIONS_MANAGER
WORKSHOP_MANAGER
QC_INSPECTOR
DRIVER
CUSTOMER
```

Browser login stores the JWT in an HttpOnly SameSite cookie. API clients can use:

```http
Authorization: Bearer <jwt>
```

## Public delivery tracking

Every delivery receives an opaque random tracking token.

Management access:

```http
GET /api/deliveries/{deliveryId}/tracking-access
```

Public tracking:

```http
GET /api/public/track/{token}
GET /api/public/track/{token}/qr
```

QR codes open:

```text
/tracking.html?token=<opaque-token>
```

The public response intentionally avoids exposing the driver's phone number or internal operational details.

## GPS tracking

```http
POST /api/deliveries/{deliveryId}/locations
GET  /api/deliveries/{deliveryId}/locations/latest
GET  /api/deliveries/{deliveryId}/locations/health
GET  /api/deliveries/{deliveryId}/locations
```

Redis can store the latest location while PostgreSQL keeps historical GPS samples. If Redis is disabled, development uses an in-memory latest-location store.

## File and POD uploads

Generic authenticated upload:

```http
POST /api/files
Content-Type: multipart/form-data
```

Proof of delivery:

```http
POST /api/deliveries/{deliveryId}/proof-of-delivery
Content-Type: multipart/form-data
```

Supported upload types include JPEG, PNG, WebP, and PDF.

## Fleet API

```text
POST /api/fleet/drivers
GET  /api/fleet/drivers
POST /api/fleet/vehicles
GET  /api/fleet/vehicles
POST /api/fleet/deliveries/{deliveryId}/assign
```

## Commercial API

```text
POST /api/commercial/work-orders/{workOrderId}/quotations
POST /api/commercial/quotations/{quotationId}/approve
POST /api/commercial/work-orders/{workOrderId}/invoices
POST /api/commercial/invoices/{invoiceId}/payments
```

## Procurement API

```text
POST  /api/procurement/suppliers
GET   /api/procurement/suppliers
POST  /api/procurement/purchase-requests
PATCH /api/procurement/purchase-requests/{id}/status
```

## Warranty and complaints API

```text
POST  /api/support/warranties
GET   /api/support/warranties
POST  /api/support/complaints
GET   /api/support/complaints
PATCH /api/support/complaints/{id}
```

## Database strategy

Local development uses H2 with Hibernate schema updates for quick startup.

Production uses PostgreSQL with:

```text
Flyway -> applies versioned migrations
Hibernate ddl-auto=validate -> verifies entity/schema compatibility
```

The initial production schema is in:

```text
src/main/resources/db/migration/V1__initial_schema.sql
```

## CI

GitHub Actions runs the Maven build and tests on every push to `main`.

```bash
mvn clean test
```

## Repository structure

```text
src/main/java/com/achyut/operation/
  api/
  asset/
  audit/
  auth/
  commercial/
  config/
  customer/
  delivery/
  fleet/
  inventory/
  procurement/
  service/
  storage/
  support/
  tracking/
  work/

src/main/resources/
  db/migration/
  static/
```

## Portfolio summary

> Built a Spring Boot operations platform for container repair and porta-cabin production that replaces manual WhatsApp-based delivery tracking with centralized GPS tracking, Redis-backed live state, WebSockets, secure public QR tracking, proof-of-delivery evidence, workflow/audit management, fleet dispatch, inventory procurement, billing, and warranty support.
