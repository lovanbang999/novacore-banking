# Product Requirements Document: NovaCore Banking Engine
**Subtitle:** Cloud-Native Double-Entry Ledger, Multi-Currency FX Engine, and Real-Time Payment System  
**Version:** 1.2.0  
**Status:** Approved / In Implementation  

---

## 1. Executive Summary & Objectives

### 1.1 Problem Statement
In retail and commercial financial infrastructure, the primary mandate is deterministic consistency, absolute financial accuracy, and zero audit loss. Common distributed systems failures—such as duplicate charging due to network retries, race conditions yielding negative balances, unauthorized cross-account access, or silent ledger mutations—are intolerable in banking domains.

Furthermore, modern neobanks and international institutions must support multi-currency accounts (`VND`, `AUD`, `USD`). In multi-currency environments, naive 2-leg accounting between mismatched currencies is mathematically invalid ($100 \text{ USD} \neq 100 \text{ VND}$). Cross-currency transfers require rigorous foreign exchange (FX) conversion with balanced, multi-leg ledger accounting through currency clearing accounts.

### 1.2 Strategic Objectives
NovaCore is an enterprise-grade core banking and payment processing engine engineered to establish high-reliability ledger semantics:
- **Double-Entry Bookkeeping with Multi-Currency Invariance:** Enforce mathematical balance invariance per currency ($\sum \text{Debits}_c == \sum \text{Credits}_c$) across customer accounts and general ledger (GL) clearing accounts using 4-leg currency conversion postings.
- **Auditable Point-in-Time FX Rates:** Append-only foreign exchange rate snapshots (`fx_rates`) ensuring every historical transaction rate is reproducible and immutable.
- **Immutable Audit Trail:** Strict append-only ledger entries (`NO UPDATE / NO DELETE` permissions).
- **Available vs. Ledger Balance:** Distinction between booked ledger balance (`currentBalance`) and spendable balance (`availableBalance`) using account holds and reservations.
- **Guaranteed Idempotency:** End-to-end duplicate execution prevention for all payment processing endpoints, supported by a scheduled reaper for abandoned in-flight requests.
- **High-Concurrency Fault Tolerance:** Deadlock-free pessimistic locking to prevent race conditions during concurrent account operations.
- **Role-Based Access Control & Identity Isolation:** JWT authentication ensuring customers can only access accounts they own, with administrative actions reserved for backoffice operators.
- **Reliable Event Streaming:** Decoupled, transactional event distribution via the Outbox Pattern and Apache Kafka for downstream consumption.

---

## 2. Personas & Stakeholders

| Persona | Role | Key Requirements |
|---|---|---|
| **Account Holder (Customer)** | Retail or commercial customer holding multi-currency accounts | Deterministic execution, cross-currency transfers with transparent FX rates, view available vs. current balance, zero unauthorized access. |
| **Backoffice Operations** | Bank operational support, FX desk, and risk administrators | Manage account lifecycle (activate, freeze, close), maintain and audit FX conversion rates, oversee ledger reconciliation per currency. |
| **Integration Client / Partner** | Third-party payment gateway or channel service | Query live FX quotes (`GET /api/v1/fx-rates`), low-latency REST APIs (<150ms), guaranteed idempotency on network retries. |
| **Compliance & Internal Audit** | Regulatory officer and internal auditor | Complete traceability of every fund movement, chart-of-accounts visibility, per-currency balance integrity verification. |

---

## 3. Scope of Work

### 3.1 In-Scope (Phase 1 & 2)
1. **Customer & Account Lifecycle Management:** Verified customer onboarding, multi-currency checking accounts (`VND`, `AUD`, `USD`), KYC activation rules, and backoffice lifecycle controls (`activate`, `freeze`, `close`).
2. **Authentication & Authorization:** JWT-based identity verification, customer ownership isolation, and Role-Based Access Control (`ROLE_CUSTOMER`, `ROLE_BACKOFFICE`).
3. **Foreign Exchange (FX) Engine:** Point-in-time append-only rate store (`fx_rates`), rate lookup service, and quote inquiry API (`GET /api/v1/fx-rates`).
4. **Multi-Currency 4-Leg Double-Entry Ledger:** Balanced debit/credit booking supporting both same-currency (2-leg) and cross-currency (4-leg via `FX_CLEARING` GL account) transfers.
5. **Balance Holds & Reservations:** Support for temporary fund reservations (`account_holds`) where `availableBalance = currentBalance - activeHolds`.
6. **Peer-to-Peer (P2P) Fund Transfers:** Atomic same-currency and cross-currency transfers with deadlock-free row locking, balance checks, and self-transfer prevention.
7. **Idempotency Gateway Layer:** Header-based idempotency keys with SHA-256 payload verification, cached response replay, and scheduled reaper for timed-out in-flight keys.
8. **Transactional Event Streaming:** Reliable domain event publishing (`TransferInitiatedEvent`, `TransferSettledEvent`, `TransferFailedEvent`) via Transactional Outbox.
9. **Automated Per-Currency Ledger Reconciliation:** Scheduled integrity checks asserting $\text{current\_balance} == \sum \text{ledger\_entries}$ per account, and $\sum \text{debits} == \sum \text{credits}$ per currency across the entire system.

### 3.2 Out-of-Scope (Explicit Architectural Boundaries)
- **Physical Card Issuing:** EMV chip profiles and ISO 7816/8583 card networks.
- **Direct External Clearing Networks:** Real-time external rails (e.g., SWIFT MT103/pacs.008, Fedwire, NPP, NAPAS) are mocked via gateway interfaces.
- **Complex Interest Engines:** Dynamic daily interest accrual calculation and term deposit compounding.

---

## 4. Functional Requirements

### Module 1: Customer & Account Lifecycle
- **FR-1.1 Customer Onboarding:** System records verified identity data (Legal Name, Tax/National ID, Email, Phone Number, KYC Status).
- **FR-1.2 Account Provisioning:** Customers can hold multiple checking accounts in supported ISO 4217 currencies (`VND`, `AUD`, `USD`). Each account is assigned an immutable, unique account number.
- **FR-1.3 Account State Machine & KYC Activation:**
  - `PENDING_KYC`: Default initial state on creation. Debit and credit operations are strictly blocked.
  - `ACTIVE`: Account is operational. Transition from `PENDING_KYC` to `ACTIVE` is **only permitted when `customer.kyc_status == 'VERIFIED'`**.
  - `FROZEN`: Administrative lock. Credit operations are permitted; debit operations are rejected.
  - `CLOSED`: Terminal state. Both credit and debit operations are rejected. A closed account cannot be reactivated or unfrozen.
- **FR-1.4 Authentication & Identity Isolation:**
  - All mutating financial endpoints require an authenticated JWT bearer token.
  - Customers (`ROLE_CUSTOMER`) can only view balances, query statements, and initiate transfers on accounts belonging to their authenticated `customerId`. Cross-customer account access is rejected with `HTTP 403 Forbidden`.
- **FR-1.5 Backoffice Account Lifecycle Operations:**
  - `PATCH /api/v1/accounts/{accountNumber}/activate`: Activates an account (requires customer KYC verified).
  - `PATCH /api/v1/accounts/{accountNumber}/freeze`: Freezes an active account.
  - `PATCH /api/v1/accounts/{accountNumber}/close`: Closes an active or frozen account (requires zero balance and zero active holds).
  - Restricted to `ROLE_BACKOFFICE` or `ROLE_ADMIN`.

### Module 2: Double-Entry Ledger & Chart of Accounts
- **FR-2.1 Balance Invariance & Available Balance:**
  - Account ledger balance (`currentBalance`) represents the sum of all settled ledger entries for that account.
  - Account available balance is dynamically computed:
    $$\text{availableBalance} = \text{currentBalance} - \sum \text{active holds}$$
  - Both `currentBalance` and `availableBalance` are exposed on account query endpoints.
- **FR-2.2 Multi-Currency Chart of Accounts & 4-Leg Postings:**
  - The ledger schema supports a Chart of Accounts (`gl_accounts`) including system accounts such as `FX_CLEARING`, `FEE_INCOME`, and `VAULT`.
  - Ledger entries are polymorphic (`account_ref_type` = `CUSTOMER` or `GL`) and record amounts in the specific currency of the corresponding account (`currency VARCHAR(3)`).
  - **Same-Currency Transfers:** Generate 2 paired ledger rows (1 DEBIT source, 1 CREDIT destination).
  - **Cross-Currency Transfers:** Generate an atomic 4-leg posting via the `FX_CLEARING` GL account:
    1. `DEBIT` source customer account in source currency (Amount = $X$)
    2. `CREDIT` `FX_CLEARING` GL account in source currency (Amount = $X$)
    3. `DEBIT` `FX_CLEARING` GL account in destination currency (Amount = $X \times \text{rate}$)
    4. `CREDIT` destination customer account in destination currency (Amount = $X \times \text{rate}$)
  - Every currency maintains exact debit-credit equality: $\sum \text{Debits}_c == \sum \text{Credits}_c$.
- **FR-2.3 Ledger Immutability:** Ledger entries are strictly append-only. No application role or process may execute `UPDATE` or `DELETE` statements on `ledger_entries`.

### Module 3: Payment, Transfer Processing & Foreign Exchange
- **FR-3.1 Transfer Validation & Routing:** Prior to executing settlement, the transfer engine must verify:
  - Source account state is `ACTIVE`.
  - Destination account state is `ACTIVE` or `FROZEN` (credits allowed).
  - Distinct accounts: `source_account_id != destination_account_id` (reject self-transfers).
  - Spendable balance sufficiency: `sourceAccount.availableBalance >= transferAmount`.
  - Authenticated principal owns the source account.
  - **Currency Routing:**
    - If `sourceAccount.currency == destinationAccount.currency`: Execute standard 2-leg transfer without FX conversion.
    - If `sourceAccount.currency != destinationAccount.currency`: Query latest effective FX rate from `fx_rates`. If no rate exists, fail with `FxRateNotAvailableException`. Compute converted destination amount rounded to standard precision and execute 4-leg settlement.
- **FR-3.2 Transfer State Machine:**
  - `INITIATED` -> `PROCESSING` -> `SETTLED` (Successful completion).
  - `INITIATED` -> `PROCESSING` -> `FAILED` (Explicit failure with recorded reason).
- **FR-3.3 FX Rate Engine & Inquiry:**
  - Point-in-time append-only rate store `fx_rates` tracking `base_currency`, `quote_currency`, `rate`, `source`, and `effective_at`.
  - Endpoint `GET /api/v1/fx-rates?base=USD&quote=VND`: Returns the latest valid exchange rate for client conversion preview.

### Module 4: Idempotency & API Reliability
- **FR-4.1 Mandatory Idempotency Key:** Mutating financial endpoints (`POST /api/v1/transfers`) require an `Idempotency-Key` HTTP header containing a unique UUID.
- **FR-4.2 Idempotency Semantics & Reaper Job:**
  - **In-flight duplicate:** Returns `HTTP 409 Conflict` if an identical key is currently executing.
  - **Completed duplicate:** Returns the original cached response body and status code without re-executing business logic or altering balances.
  - **Mismatched payload:** Returns `HTTP 422 Unprocessable Entity` if the same key is reused with a different request payload hash.
  - **Scheduled Idempotency Reaper:** Background worker identifies records stuck in `PENDING` for longer than 30 seconds and marks them `FAILED` (HTTP 504), unblocking subsequent retry attempts.

### Module 5: Transaction History & Statements
- **FR-5.1 Historical Querying:** Support range-based statement generation with pagination.
- **FR-5.2 Running Balance:** Each statement line item includes the running balance immediately following that transaction in the account's native currency.

---

## 5. Non-Functional Requirements

- **NFR-1 Data Consistency (ACID):** Transaction isolation must be configured at `READ COMMITTED` or higher, combined with deterministic row-level locks (`SELECT ... FOR UPDATE`) ordered by account identifier on customer accounts.
- **NFR-2 Strict Idempotency:** Zero duplicate ledger entries or balance deductions under network timeout, retry storms, or client retries.
- **NFR-3 Throughput & Latency:** 95th percentile latency (P95) for internal transfers must remain under 150ms under sustained loads of 200 Transactions Per Second (TPS).
- **NFR-4 Continuous Per-Currency Ledger Reconciliation:** Automated reconciliation jobs run periodically and in integration test suites to verify that:
  1. $\text{accounts.current\_balance} == \sum \text{ledger\_entries}$ per customer account.
  2. For every currency $c$, $\sum \text{Debits}_c == \sum \text{Credits}_c$ across the entire ledger.
  Any discrepancy raises critical audit alerts.
- **NFR-5 Asynchronous Resilience:** Outbox pattern guarantees at-least-once delivery to Apache Kafka. Transfer commits must succeed even if Kafka brokers are temporarily unreachable.
- **NFR-6 Distributed Caching & Fast Locking (Redis):** Redis serves as a read-through cache for account queries and FX rates, and provides distributed locking capabilities.

---

## 6. Milestone Schedule

```
Sprint 0:   Infrastructure & Project Setup (Java 21, PostgreSQL 16, Redis 7, Flyway Migrations, Seed FX Rates)
Sprint 1:   Customer & Account Lifecycle (Entities, Holds, State Machine, Backoffice Endpoints)
Sprint 1.5: Security & Access Control (JWT Authentication, Customer Isolation & Backoffice RBAC)
Sprint 2:   Atomic Transfer Engine, FX Conversion & 4-Leg Ledger (Same-Currency 2-Leg & Cross-Currency 4-Leg)
Sprint 3:   API Reliability & Idempotency Layer (Header Filter, Replay, Scheduled Reaper Job)
Sprint 4:   Event-Driven Architecture (Transactional Outbox + Kafka + Consumers)
Sprint 5:   Concurrency, Per-Currency Reconciliation & Reliability Testing (Testcontainers, Drift Tests)
Sprint 6:   Production Hardening, Observability & Documentation (Swagger, Actuator, README)
```
