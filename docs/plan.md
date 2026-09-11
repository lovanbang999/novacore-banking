# NovaCore Banking Engine: Milestone Execution Plan

**Project:** NovaCore Banking Engine  
**Target Architecture:** Java 21 LTS · Spring Boot 4.x / 3.x · PostgreSQL 16 · Redis 7 · Apache Kafka · Docker  
**Target Outcome:** Production-grade portfolio demonstrating Double-Entry Ledger, Multi-Currency FX Engine, Concurrency Control, Idempotency, and Event-Driven Architecture for Tier-1 Banking Engineering roles (NAB, Techcombank, VPBank).  
**Related Documents:**  
- [PRD.md](file:///home/pang/work-space/research/novacore-banking/docs/PRD.md) — Product requirements and business rules (v1.2.0)  
- [TECHNICAL_DESIGN.md](file:///home/pang/work-space/research/novacore-banking/docs/TECHNICAL_DESIGN.md) — System architecture, DDL schema, and concurrency workflows (v1.2.0)  

---

## Milestone Roadmap Overview

```
[Sprint 0]   Bootstrap & Database Setup (PostgreSQL + Redis + Flyway + Seed FX Rates) [COMPLETED]
    ↓
[Sprint 1]   Customer & Account Lifecycle (Entities, Holds, State Machine, Backoffice Endpoints)
    ↓
[Sprint 1.5] Security & Access Control (JWT Authentication, Customer Isolation & RBAC)
    ↓
[Sprint 2]   Atomic Transfer Engine, FX Conversion & 4-Leg Ledger (Same-Currency 2-Leg & Cross-Currency 4-Leg)
    ↓
[Sprint 3]   Idempotent Payment Gateway & Reaper (Fast Redis Lock, Replay, 30s Timeout Reaper)
    ↓
[Sprint 4]   Event-Driven Architecture (Transactional Outbox + Kafka)
    ↓
[Sprint 5]   Concurrency, Per-Currency Reconciliation & Reliability Testing (Drift Tests, Testcontainers)
    ↓
[Sprint 6]   Observability, Documentation & Portfolio Polish (Swagger, Actuator, README)
```

---

## Detailed Milestones & Checklists

### Sprint 0: Infrastructure & Project Setup
**Goal:** Get the project skeleton running with Java 21 Virtual Threads, Docker PostgreSQL 16, Redis 7, baseline Flyway migration, and seed FX rates.

- [x] Install OpenJDK 21 LTS (`openjdk 21.0.12`).
- [x] Scaffold Spring Boot project using Gradle wrapper (`novacore-banking/`).
- [x] Create `novacore-banking/docker-compose.yml` with PostgreSQL 16, Redis 7, and Adminer.
- [x] Start database and Redis containers (`docker compose up -d`).
- [x] Configure `src/main/resources/application.yml` with Virtual Threads, PostgreSQL, and Redis connection.
- [x] Create baseline Flyway migration `V1__init_schema.sql` (Customers, GL Accounts with `FX_CLEARING`, Accounts with `PENDING_KYC` default, Holds, FX Rates, Transactions, Multi-Currency Ledger Entries, Idempotency, Outbox).
- [x] Verify execution: Run `./gradlew bootRun` and confirm Flyway successfully creates and seeds all tables.

**Definition of Done (DoD):**
- Spring Boot boots on `localhost:8080` with zero migration errors.
- PostgreSQL has all 9 core tables populated with seed GL accounts and initial FX rates.

---

### Sprint 1: Customer & Account Lifecycle
**Goal:** Implement the Account Bounded Context following Domain-Driven Design (DDD), account holds, and backoffice lifecycle management.

- [ ] **Domain Layer (`com.novacore.banking.account.domain`):**
  - [ ] `AccountStatus` enum (`PENDING_KYC`, `ACTIVE`, `FROZEN`, `CLOSED`) with state transition logic.
  - [ ] `AccountType` enum (`CHECKING`, `SAVINGS`).
  - [ ] `HoldStatus` enum (`ACTIVE`, `RELEASED`, `CAPTURED`).
  - [ ] `Customer` entity (`id`, `fullName`, `email`, `phoneNumber`, `kycStatus`).
  - [ ] `Account` entity (`id`, `customerId`, `accountNumber`, `currency`, `currentBalance`, `status = PENDING_KYC`, `@Version Long version`).
  - [ ] `AccountHold` entity (`id`, `accountId`, `amount`, `reason`, `status`, `expiresAt`).
- [ ] **Infrastructure Layer (`com.novacore.banking.account.infrastructure`):**
  - [ ] `CustomerRepository` (Spring Data JPA).
  - [ ] `AccountRepository` (Spring Data JPA).
  - [ ] `AccountHoldRepository` (Spring Data JPA — find active holds by account).
- [ ] **Application Layer (`com.novacore.banking.account.application`):**
  - [ ] `CreateAccountCommand` (DTO with Bean Validation).
  - [ ] `AccountResponse` (DTO exposing `currentBalance`, `availableBalance`, and `activeHoldsAmount`).
  - [ ] `AccountService`:
    - [ ] Create account in `PENDING_KYC` status.
    - [ ] Calculate `availableBalance = currentBalance - SUM(active holds)`.
  - [ ] Backoffice Lifecycle Use Cases:
    - [ ] `ActivateAccountUseCase`: Transitions `PENDING_KYC` -> `ACTIVE` **only if `customer.kyc_status == 'VERIFIED'`**; also unfreezes `FROZEN` -> `ACTIVE`. Rejects activation if account is `CLOSED`.
    - [ ] `FreezeAccountUseCase`: Transitions `ACTIVE` -> `FROZEN`.
    - [ ] `CloseAccountUseCase`: Transitions `ACTIVE` or `FROZEN` -> `CLOSED` (requires zero balance and zero active holds).
- [ ] **REST API Layer (`com.novacore.banking.account.infrastructure.web`):**
  - [ ] `POST /api/v1/accounts` — Open a new checking account (initial state: `PENDING_KYC`).
  - [ ] `GET /api/v1/accounts/{accountNumber}` — Query account balance (returns `currentBalance` and `availableBalance`).
  - [ ] `PATCH /api/v1/accounts/{accountNumber}/activate` — Backoffice activation.
  - [ ] `PATCH /api/v1/accounts/{accountNumber}/freeze` — Backoffice freeze.
  - [ ] `PATCH /api/v1/accounts/{accountNumber}/close` — Backoffice closure.
  - [ ] Global exception handling (`AccountNotFoundException`, `KycNotVerifiedException`, `IllegalStateTransitionException`).

**Definition of Done (DoD):**
- Accounts are created with `PENDING_KYC` by default.
- Attempting to activate an account with unverified customer KYC returns HTTP 400/422.
- Available balance dynamically accounts for active holds.
- Backoffice endpoints freeze, close, and activate accounts according to state machine rules.

*Estimated Effort Impact:* +1.5 - 2 days.

---

### Sprint 1.5: Security & Access Control (JWT & RBAC)
**Goal:** Implement stateless JWT authentication, customer account-ownership isolation, and backoffice role enforcement.

- [ ] **Security Infrastructure (`com.novacore.banking.shared.security`):**
  - [ ] Add `spring-boot-starter-security` and `jjwt` dependencies to `build.gradle`.
  - [ ] `JwtTokenProvider`: Generates and validates tokens containing `sub` (username/email), `customerId`, and `roles`.
  - [ ] `JwtAuthenticationFilter`: Extracts Bearer token, populates Spring `SecurityContext`.
  - [ ] `SecurityConfig`: Stateless session management, permit `/actuator/**` and `/api/v1/auth/**`, protect all banking endpoints.
- [ ] **Role-Based Access Control (RBAC):**
  - [ ] Define roles: `ROLE_CUSTOMER`, `ROLE_BACKOFFICE`, `ROLE_ADMIN`.
  - [ ] Restrict backoffice endpoints (`PATCH /api/v1/accounts/{accountNumber}/*`) to `@PreAuthorize("hasRole('BACKOFFICE')")`.
- [ ] **Customer Account Ownership Enforcement:**
  - [ ] Add ownership validation interceptor/guard: When a customer invokes `GET /api/v1/accounts/{accountNumber}` or initiates a transfer, verify that `account.customer_id == principal.customerId`.
  - [ ] Reject cross-customer access with HTTP 403 Forbidden.
- [ ] **Mutating Endpoint Security:**
  - [ ] Ensure all financial endpoints (`POST /api/v1/transfers`, `POST /api/v1/accounts`) require an authenticated principal.

**Definition of Done (DoD):**
- Unauthenticated requests return HTTP 401 Unauthorized.
- Customer A attempting to query or transfer from Customer B's account returns HTTP 403 Forbidden.
- Only users with `ROLE_BACKOFFICE` can activate, freeze, or close accounts.

*Estimated Effort Impact:* 3 - 4 days.

---

### Sprint 2: Atomic Transfer Engine, FX Conversion & 4-Leg Ledger
**Goal:** Implement the core banking transfer engine supporting same-currency (2-leg) and cross-currency foreign exchange (4-leg) settlements.

- [ ] **FX Domain & Rates (`com.novacore.banking.fx`):**
  - [ ] `FxRate` entity (`id`, `baseCurrency`, `quoteCurrency`, `rate`, `source`, `effectiveAt`).
  - [ ] `FxRateRepository` (query latest active rate by base/quote pair).
  - [ ] `FxRateService`: Look up exchange rate, calculate converted amounts with standard precision rounding.
  - [ ] `FxRateNotAvailableException` thrown if rate is missing.
  - [ ] `GET /api/v1/fx-rates?base=USD&quote=VND` query endpoint.
- [ ] **Chart of Accounts & Ledger Domain (`com.novacore.banking.ledger`):**
  - [ ] `GlAccount` entity (`id`, `code`, `accountType`, `name`).
  - [ ] `EntryType` enum (`DEBIT`, `CREDIT`).
  - [ ] `AccountRefType` enum (`CUSTOMER`, `GL`).
  - [ ] `LedgerEntry` entity (`id`, `transactionId`, `accountId`, `accountRefType`, `entryType`, `amount`, `currency`, `runningBalance`, `fxRateApplied`, `baseCurrencyEquivalent`).
  - [ ] `LedgerEntryRepository` and `GlAccountRepository`.
- [ ] **Payment Domain & Transaction (`com.novacore.banking.payment.domain`):**
  - [ ] `Transaction` entity (`referenceNumber`, `sourceAccountId`, `destinationAccountId`, `sourceAmount`, `sourceCurrency`, `destinationAmount`, `destinationCurrency`, `fxRateApplied`, `status`).
  - [ ] `TransferStatus` enum (`INITIATED`, `SETTLED`, `FAILED`).
  - [ ] `SelfTransferException` and `InsufficientFundsException`.
  - [ ] `TransactionRepository`.
- [ ] **Transfer Application Service (`com.novacore.banking.payment.application`):**
  - [ ] `@Transactional(isolation = Isolation.READ_COMMITTED)` orchestration:
    1. Authenticated principal ownership verification.
    2. Self-transfer check: `sourceAccountId != destinationAccountId`.
    3. Spendable balance check: `sourceAccount.availableBalance >= sourceAmount`.
    4. Acquire deterministic pessimistic write locks on both customer accounts ordered by ascending UUID. *(Note: GL accounts are append-only and do not acquire row locks)*.
    5. Currency Routing:
       - **Same-Currency Transfer:** Deduct source balance, credit destination balance, record 2 paired ledger rows (1 DEBIT source, 1 CREDIT destination).
       - **Cross-Currency Transfer:** Fetch FX rate, calculate destination amount, record 4 paired ledger rows:
         - Leg 1: DEBIT source account (source currency, amount = $X$)
         - Leg 2: CREDIT `FX_CLEARING` GL account (source currency, amount = $X$)
         - Leg 3: DEBIT `FX_CLEARING` GL account (destination currency, amount = $X \times \text{rate}$)
         - Leg 4: CREDIT destination account (destination currency, amount = $X \times \text{rate}$)
    6. Save `Transaction` record with source/dest amounts and `fx_rate_applied`.
    7. Enqueue `TransferSettledEvent` into `outbox_events`.
    8. Evict Redis cache for both accounts (`@CacheEvict`).
- [ ] **REST API:**
  - [ ] `POST /api/v1/transfers` (Response returns `sourceAmount`, `sourceCurrency`, `destinationAmount`, `destinationCurrency`, and `fxRateApplied`).
  - [ ] `GET /api/v1/accounts/{accountNumber}/statement` (Statements in account's native currency).

**Definition of Done (DoD):**
- Same-currency transfers generate exactly 2 ledger entries.
- Cross-currency transfers generate exactly 4 ledger entries with balanced debits and credits per currency.
- Missing FX rate triggers `FxRateNotAvailableException` (HTTP 422) with full rollback.
- Self-transfers are rejected by service validation and DB check constraint.

*Estimated Effort Impact:* +2.5 - 3 days (due to point-in-time FX lookup, precision rounding, 4-leg ledger logic, and cross-currency response payload).

---

### Sprint 3: API Reliability & Idempotency Layer
**Goal:** Guarantee that retries and network timeouts never cause double-spending, supported by a fast Redis lock and scheduled reaper job.

- [ ] **Idempotency Persistence (`com.novacore.banking.shared.idempotency`):**
  - [ ] `IdempotencyRecord` entity (`key`, `requestHash`, `status`, `responseBody`, `httpStatusCode`, `expiresAt`).
  - [ ] `IdempotencyRepository` with index query for pending records.
- [ ] **Two-Tier Idempotency Filter (`IdempotencyFilter`):**
  - [ ] Extract `Idempotency-Key` header (return HTTP 400 if missing on mutating routes).
  - [ ] Acquire Redis distributed lock (`SETNX key 30s`) to reject immediate concurrent duplicates before hitting DB.
  - [ ] Compute SHA-256 hash of canonical request body.
  - [ ] If key exists with status `COMPLETED`: return cached response body (HTTP 200/201).
  - [ ] If key exists with status `PENDING`: return HTTP 409 Conflict.
  - [ ] If new: insert `PENDING`, execute business service, record response, mark `COMPLETED`.
- [ ] **Scheduled Idempotency Reaper Job:**
  - [ ] Background worker (`@Scheduled(fixedDelayString = "${novacore.idempotency.reaper.fixed-rate-ms:5000}")`).
  - [ ] Queries `idempotency_records` where `status = 'PENDING'` and `created_at < NOW() - 30 seconds`.
  - [ ] Marks timed-out records as `FAILED` (HTTP 504 Gateway Timeout), unblocking future retries.

**Definition of Done (DoD):**
- Identical requests with same `Idempotency-Key` return identical cached response with only 1 ledger booking.
- Simulated stuck transaction is automatically reaped after 30s, allowing client to retry successfully.

*Estimated Effort Impact:* +1 day.

---

### Sprint 4: Event-Driven Architecture (Transactional Outbox + Kafka)
**Goal:** Decouple downstream consumers (Notification, Audit) reliably without distributed dual-write failure modes.

- [ ] **Kafka Infrastructure:**
  - [ ] Add Apache Kafka service to `docker-compose.yml`.
  - [ ] Add `spring-kafka` dependency to `build.gradle`.
- [ ] **Transactional Outbox Implementation (`com.novacore.banking.shared.outbox`):**
  - [ ] `OutboxEvent` entity (`aggregateType`, `aggregateId`, `eventType`, `payload`, `status`).
  - [ ] Enqueue `TransferSettledEvent` within the transfer database transaction.
- [ ] **Outbox Scheduled Publisher:**
  - [ ] Worker (`@Scheduled(fixedDelay = 200)`) querying `status = 'PENDING'`.
  - [ ] Publish payload to Kafka topic `banking.transfers`.
  - [ ] Mark outbox record `status = 'PUBLISHED'` upon broker acknowledgment (ACK).
- [ ] **Consumer Service Demo:**
  - [ ] `NotificationConsumer` listening to `banking.transfers` logging mock push/email notifications.

**Definition of Done (DoD):**
- Outbox events queue safely during broker downtime and publish upon reconnection (At-Least-Once Delivery).

---

### Sprint 5: Concurrency, Per-Currency Reconciliation & Reliability Testing
**Goal:** Write rigorous automated tests proving financial consistency, multi-currency ledger balance integrity, and security isolation under high concurrency.

- [ ] **Unit Tests:**
  - [ ] Domain logic tests (state machine transitions, negative amounts, KYC validation).
  - [ ] Cross-currency 4-leg unit test: Verify exactly 4 ledger entries with matching currencies on each leg.
  - [ ] FX missing rate test: Verify `FxRateNotAvailableException` triggers complete rollback (zero balance change, zero ledger entries).
- [ ] **Automated Continuous Per-Currency Ledger Reconciliation Job & Test:**
  - [ ] `LedgerReconciliationService`: Checks that for every currency $c$, $\sum \text{Debits}_c == \sum \text{Credits}_c$ across the entire ledger.
  - [ ] Customer balance test: Asserts $\text{current\_balance} == \sum \text{ledger\_entries}$ per customer account.
  - [ ] Test verifying that an intentional manual balance alteration triggers `LedgerDriftException` and logs critical audit alerts.
- [ ] **Multi-Threaded Concurrency Test (`AccountConcurrencyTest`):**
  - [ ] 10 parallel threads attempt withdrawals against limited available balance.
  - [ ] Asserts zero negative balance and zero lost updates.
- [ ] **Idempotency & Reaper Test:**
  - [ ] Concurrently posts identical request with same key. Asserts single debit.
  - [ ] Asserts stuck `PENDING` key transitions to `FAILED` after reaper execution.
- [ ] **Security & RBAC Integration Tests:**
  - [ ] Customer attempting cross-customer transfer receives HTTP 403.
  - [ ] Customer attempting backoffice freeze/activate endpoint receives HTTP 403.
  - [ ] Unauthenticated requests receive HTTP 401.
- [ ] **Testcontainers Integration:**
  - [ ] Execute test suite against real containerized PostgreSQL and Redis.

**Definition of Done (DoD):**
- `./gradlew test` passes 100% with concurrency, per-currency reconciliation, FX, and security tests included.

*Estimated Effort Impact:* +2 days (due to per-currency reconciliation and FX tests).

---

### Sprint 6: Production Hardening, Observability & Portfolio Polish
**Goal:** Transform the codebase into an enterprise showcase for tier-1 banking engineering roles.

- [ ] **Observability:**
  - [ ] Spring Boot Actuator endpoints (`/actuator/health`, `/actuator/metrics`, `/actuator/prometheus`).
  - [ ] Structured JSON logging with Correlation ID (`X-Correlation-ID`).
- [ ] **API Documentation:**
  - [ ] Swagger UI OpenAPI 3 documentation (`/swagger-ui.html`).
- [ ] **GitHub Portfolio Presentation (`README.md`):**
  - [ ] Update README with Multi-Currency FX Engine, 4-Leg Ledger Architecture, Chart of Accounts, and Redis usage.
  - [ ] Include concurrency benchmark results and per-currency ledger reconciliation verification screenshots.

---

## Progress Tracker & Effort Impact Summary

| Milestone | Status | Key Deliverables | Estimated Effort Delta |
|---|---|---|---|
| **Sprint 0: Setup & DB** | Completed | Java 21, Docker (PostgreSQL 16, Redis 7), Flyway V1 (GL, FX, Holds) | Baseline |
| **Sprint 1: Account Domain** | In Progress | Customer, Account (`PENDING_KYC`), Holds, Backoffice APIs | +1.5 - 2 Days |
| **Sprint 1.5: Security & RBAC** | In Progress | JWT Authentication, Customer Isolation, Backoffice RBAC | +3 - 4 Days |
| **Sprint 2: Transfer, FX & Ledger** | Not Started | Same-Currency (2-leg) & Cross-Currency FX (4-leg) Settlements | +2.5 - 3 Days |
| **Sprint 3: Idempotency** | Not Started | Redis Fast Lock, Request Replay, 30s Timeout Reaper | +1 Day |
| **Sprint 4: Outbox & Kafka** | Not Started | Transactional Outbox, Kafka Publisher, Notification Consumer | Baseline |
| **Sprint 5: Tests & Reconciliation**| Not Started | Concurrency Tests, Per-Currency Ledger Reconciliation, FX Tests | +2 Days |
| **Sprint 6: Polish & Portfolio** | Not Started | Swagger UI, Actuator, Multi-Currency Documentation Polish | Baseline |

**Total Estimated Effort Impact:** ~10 - 11.5 additional engineering days across the roadmap. The inclusion of full Foreign Exchange (FX) with balanced 4-leg GL clearing positions NovaCore as an authentic multi-currency core banking engine.
