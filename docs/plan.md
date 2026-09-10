# NovaCore Banking Engine: Milestone Execution Plan

**Project:** NovaCore Banking Engine  
**Target Architecture:** Java 21 LTS · Spring Boot 4.x / 3.x · PostgreSQL 16 · Apache Kafka · Redis · Docker  
**Target Outcome:** Production-grade portfolio demonstrating Double-Entry Ledger, Concurrency Control, Idempotency, and Event-Driven Architecture for Tier-1 Banking Engineering roles (NAB, Techcombank, VPBank).  
**Related Documents:**  
- [PRD.md](file:///home/pang/work-space/research/novacore-banking/docs/PRD.md) — Product requirements and business rules  
- [TECHNICAL_DESIGN.md](file:///home/pang/work-space/research/novacore-banking/docs/TECHNICAL_DESIGN.md) — System architecture, DDL schema, and concurrency workflows  

---

## Milestone Roadmap Overview

```
[Sprint 0] Bootstrap & Database Setup (PostgreSQL + Flyway) [COMPLETED]
    ↓
[Sprint 1] Customer & Account Lifecycle (DDD Entity & Service)
    ↓
[Sprint 2] Atomic Transfer & Double-Entry Ledger (Deadlock-Free Locking)
    ↓
[Sprint 3] Idempotent Payment Gateway (Filter & Replay)
    ↓
[Sprint 4] Event-Driven Architecture (Transactional Outbox + Kafka)
    ↓
[Sprint 5] Concurrency & Reliability Testing (Testcontainers & Race-Condition Tests)
    ↓
[Sprint 6] Observability, Documentation & Portfolio Polish (Swagger, Actuator, README)
```

---

## Detailed Milestones & Checklists

### Sprint 0: Infrastructure & Project Setup
**Goal:** Get the project skeleton running with Java 21 Virtual Threads, Docker PostgreSQL, and Flyway migration.

- [x] Install OpenJDK 21 LTS (`openjdk 21.0.12`).
- [x] Scaffold Spring Boot project using Gradle wrapper (`novacore-banking/`).
- [x] Create `novacore-banking/docker-compose.yml` with PostgreSQL 16 and Adminer.
- [x] Start database container (`docker compose up -d`).
- [x] Configure `src/main/resources/application.yml` with `spring.threads.virtual.enabled=true` and database connection.
- [x] Create initial Flyway migration `src/main/resources/db/migration/V1__init_schema.sql`.
- [x] Verify execution: Run `./gradlew bootRun` and confirm Flyway successfully creates tables.

**Definition of Done (DoD):**
- Spring Boot boots on `localhost:8080`.
- PostgreSQL container has tables: `customers`, `accounts`, `transactions`, `ledger_entries`, `idempotency_records`, `outbox_events`.

---

### Sprint 1: Customer & Account Lifecycle
**Goal:** Implement the Account Bounded Context following Domain-Driven Design (DDD).

- [ ] **Domain Layer (`com.novacore.banking.account.domain`):**
  - [ ] `AccountStatus` enum (`ACTIVE`, `FROZEN`, `CLOSED`).
  - [ ] `AccountType` enum (`CHECKING`, `SAVINGS`).
  - [ ] `Customer` entity (`id`, `fullName`, `email`, `phoneNumber`, `kycStatus`).
  - [ ] `Account` entity (`id`, `customerId`, `accountNumber`, `currency`, `currentBalance`, `@Version Long version`).
- [ ] **Infrastructure Layer (`com.novacore.banking.account.infrastructure`):**
  - [ ] `CustomerRepository` (Spring Data JPA).
  - [ ] `AccountRepository` (Spring Data JPA).
- [ ] **Application Layer (`com.novacore.banking.account.application`):**
  - [ ] `CreateAccountCommand` (DTO with Bean Validation annotations).
  - [ ] `AccountResponse` (DTO).
  - [ ] `AccountService` (Create account, validate KYC, generate unique account numbers).
- [ ] **REST API Layer (`com.novacore.banking.account.infrastructure.web`):**
  - [ ] `POST /api/v1/accounts` — Open a new checking account.
  - [ ] `GET /api/v1/accounts/{accountNumber}` — Query account details & balance.
  - [ ] Global exception handling (`GlobalExceptionHandler` for `AccountNotFoundException`, `DuplicateAccountException`).

**Definition of Done (DoD):**
- Can create an account via `curl` / Postman and retrieve its balance.
- Input validation rejects negative balance, blank names, or invalid emails with HTTP 400.

---

### Sprint 2: Atomic Transfer Engine & Double-Entry Ledger
**Goal:** Implement the core banking transfer engine with double-entry bookkeeping and deadlock-free pessimistic locking.

- [ ] **Ledger Domain & Repository (`com.novacore.banking.ledger`):**
  - [ ] `EntryType` enum (`DEBIT`, `CREDIT`).
  - [ ] `LedgerEntry` entity (`id`, `transactionId`, `accountId`, `entryType`, `amount`, `runningBalance`, `description`).
  - [ ] `LedgerEntryRepository` (Append-only: save and query by account).
- [ ] **Payment Domain & Transaction (`com.novacore.banking.payment.domain`):**
  - [ ] `Transaction` entity (`referenceNumber`, `sourceAccountId`, `destinationAccountId`, `amount`, `status`, `failureReason`).
  - [ ] `TransferStatus` enum (`INITIATED`, `SETTLED`, `FAILED`).
  - [ ] `TransactionRepository`.
- [ ] **Concurrency Control in `AccountRepository`:**
  - [ ] Implement `findAccountsForUpdate(UUID id1, UUID id2)` ordered by UUID to prevent two-way deadlocks.
- [ ] **Transfer Application Service (`com.novacore.banking.payment.application`):**
  - [ ] `@Transactional(isolation = Isolation.READ_COMMITTED)` orchestration:
    1. Lock both accounts in sorted ID order.
    2. Check source account status (`ACTIVE`) and balance (`balance >= amount`).
    3. Deduct source balance, credit destination balance.
    4. Save `Transaction` (`status = SETTLED`).
    5. Save 2 `LedgerEntry` records (1 DEBIT, 1 CREDIT).
    6. Rollback cleanly if balance is insufficient.
- [ ] **REST API:**
  - [ ] `POST /api/v1/transfers` (Initiate P2P fund transfer).
  - [ ] `GET /api/v1/accounts/{accountNumber}/statement` (Query statement with running balances).

**Definition of Done (DoD):**
- Transferring 100,000 VND from Account A to Account B results in exactly two ledger entries.
- Account balances update atomically.
- Transfers failing due to insufficient funds do not create partial balance deductions.

---

### Sprint 3: API Reliability & Idempotency Layer
**Goal:** Guarantee that network retries and duplicate user clicks never cause double-spending.

- [ ] **Idempotency Entity & Repository (`com.novacore.banking.shared.idempotency`):**
  - [ ] `IdempotencyRecord` entity (`key`, `requestHash`, `status`, `responseBody`, `httpStatusCode`, `expiresAt`).
  - [ ] `IdempotencyRepository`.
- [ ] **Idempotency Interceptor / Filter (`IdempotencyFilter`):**
  - [ ] Intercept all mutating endpoints (`POST /api/v1/transfers`).
  - [ ] Extract `Idempotency-Key` header. Return `400 Bad Request` if missing.
  - [ ] Compute SHA-256 hash of the request payload.
  - [ ] If key exists with status `COMPLETED`: replay cached JSON response immediately (HTTP 200/201).
  - [ ] If key exists with status `PENDING`: return `409 Conflict` ("Transaction currently being processed").
  - [ ] If new: insert `PENDING`, execute request, save response JSON, mark `COMPLETED`.

**Definition of Done (DoD):**
- Sending the identical transfer request 3 times with the same `Idempotency-Key` charges the user **only once** and returns the exact same response for the subsequent 2 requests.

---

### Sprint 4: Event-Driven Architecture (Transactional Outbox + Kafka)
**Goal:** Decouple downstream consumers (Notification, Analytics) reliably without distributed transaction risks.

- [ ] **Kafka Infrastructure:**
  - [ ] Add Apache Kafka & Zookeeper / KRaft to `docker-compose.yml`.
  - [ ] Add `spring-kafka` dependency to `build.gradle`.
- [ ] **Transactional Outbox Implementation (`com.novacore.banking.shared.outbox`):**
  - [ ] `OutboxEvent` entity (`aggregateType`, `aggregateId`, `eventType`, `payload`, `status`).
  - [ ] Insert `TransferSettledEvent` into `outbox_events` inside the transfer DB transaction.
- [ ] **Outbox Scheduled Publisher:**
  - [ ] Background worker (`@Scheduled(fixedDelay = 200)`) querying `status = 'PENDING'`.
  - [ ] Publish payload to Kafka topic `banking.transfers`.
  - [ ] Mark outbox record `status = 'PUBLISHED'` upon Kafka ACK.
- [ ] **Consumer Service Demo:**
  - [ ] `NotificationConsumer` listening to `banking.transfers` and logging mock SMS/Email notifications.

**Definition of Done (DoD):**
- If Kafka is temporarily shut down, fund transfers still succeed and events queue safely in `outbox_events`.
- Once Kafka restarts, pending events are automatically published (At-Least-Once Delivery).

---

### Sprint 5: Concurrency & Reliability Testing
**Goal:** Write rigorous automated tests proving financial consistency under high-load race conditions.

- [ ] **Unit Tests:**
  - [ ] Domain logic tests (disallow negative transfer amount, test balance arithmetic).
- [ ] **Multi-Threaded Concurrency Test (`AccountConcurrencyTest`):**
  - [ ] Setup an account with `1,000,000 VND`.
  - [ ] Spawn 10 parallel threads using `ExecutorService` and `CountDownLatch`, each trying to withdraw `200,000 VND` at the exact same millisecond.
  - [ ] Assert: Exactly 5 requests succeed, 5 fail with `InsufficientFundsException`.
  - [ ] Assert: Final balance is exactly `0 VND` (zero negative balance, zero lost updates).
- [ ] **Idempotency Multi-Threaded Test:**
  - [ ] Send 10 concurrent requests sharing the same `Idempotency-Key`.
  - [ ] Assert: Database contains exactly 1 transfer and 2 ledger entries.
- [ ] **Testcontainers Setup:**
  - [ ] Run integration tests against real containerized PostgreSQL to avoid in-memory H2 divergence.

**Definition of Done (DoD):**
- `./gradlew test` passes 100% with concurrency and idempotency tests included.

---

### Sprint 6: Production Hardening, Observability & Portfolio Polish
**Goal:** Transform the codebase into a standout showcase for banking job applications.

- [ ] **Observability:**
  - [ ] Configure Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`).
  - [ ] Structured JSON logging with Correlation ID / Trace ID in HTTP headers (`X-Correlation-ID`).
- [ ] **API Documentation:**
  - [ ] Add `springdoc-openapi-starter-webmvc-ui` (Swagger UI at `/swagger-ui.html`).
- [ ] **GitHub Portfolio Presentation (`README.md`):**
  - [ ] Architecture diagram (ASCII / Mermaid).
  - [ ] Key design decisions: Double-entry ledger, Pessimistic locking strategy, Transactional Outbox.
  - [ ] Concurrency test results benchmark.
  - [ ] Quickstart instructions (`docker compose up` + `./gradlew bootRun`).

---

## Progress Tracker

| Milestone | Status | Key Deliverables |
|---|---|---|
| **Sprint 0: Setup & DB** | Completed | Java 21, Docker Compose, Flyway V1 Migration |
| **Sprint 1: Account Domain** | In Progress | Customer & Account Entities, Services, REST APIs |
| **Sprint 2: Transfer & Ledger** | Not Started | Double-Entry Bookkeeping, Concurrency Lock, Statements |
| **Sprint 3: Idempotency** | Not Started | Idempotency Interceptor, Request replay |
| **Sprint 4: Outbox & Kafka** | Not Started | Transactional Outbox, Kafka Publisher, Notification Consumer |
| **Sprint 5: Concurrency Tests** | Not Started | Multi-threaded race-condition test, Testcontainers |
| **Sprint 6: Polish & Portfolio** | Not Started | Swagger UI, Actuator, GitHub README |
