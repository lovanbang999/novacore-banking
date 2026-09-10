# Product Requirements Document: NovaCore Banking Engine
**Subtitle:** Cloud-Native Double-Entry Ledger and Real-Time Payment Processing System  
**Version:** 1.0.0  
**Status:** Approved / In Implementation  

---

## 1. Executive Summary & Objectives

### 1.1 Problem Statement
In retail and commercial financial infrastructure, the primary mandate is deterministic consistency, absolute financial accuracy, and zero audit loss. Common distributed systems failures—such as duplicate charging due to network retries, race conditions yielding negative balances, or out-of-order state mutations—are intolerable in banking domains. 

Legacy core banking systems often couple ledger state with application logic, relying on mutable balance records that obscure the true financial audit trail.

### 1.2 Strategic Objectives
NovaCore is an enterprise-grade core banking and payment processing engine engineered to establish high-reliability ledger semantics:
- **Double-Entry Bookkeeping:** Enforce mathematical balance invariance (`Sum(Debits) == Sum(Credits)`) across all monetary movements.
- **Immutable Audit Trail:** Strict append-only ledger entries (`NO UPDATE / NO DELETE` permissions).
- **Guaranteed Idempotency:** End-to-end duplicate execution prevention for all payment processing endpoints.
- **High-Concurrency Fault Tolerance:** Deadlock-free pessimistic locking to prevent race conditions during concurrent account operations.
- **Reliable Event Streaming:** Decoupled, transactional event distribution via the Outbox Pattern and Apache Kafka for downstream consumption.

---

## 2. Personas & Stakeholders

| Persona | Role | Key Requirements |
|---|---|---|
| **Account Holder** | Retail or commercial customer initiating transfers | Deterministic execution, accurate real-time balances, immediate transaction feedback. |
| **Integration Client / Partner** | Third-party payment gateway or internal channel service | Low-latency REST APIs (<150ms), guaranteed idempotency on network timeouts, clear error codes. |
| **Compliance & Internal Audit** | Regulatory officer and internal auditor | Complete traceability of every fund movement, verifiable mathematical balance integrity. |
| **Backoffice Operations** | Bank operational support engineer | Account lifecycle controls (freeze, close, activate), historical statement generation. |

---

## 3. Scope of Work

### 3.1 In-Scope (Phase 1 & 2)
1. **Customer & Account Lifecycle Management:** Customer profiles, multi-currency checking accounts, state machine enforcement (`PENDING_KYC`, `ACTIVE`, `FROZEN`, `CLOSED`).
2. **Double-Entry Ledger Engine:** Balanced debit/credit booking, running balance calculations, system reconciliation.
3. **Peer-to-Peer (P2P) Fund Transfers:** Atomic account-to-account transfers with strict concurrency isolation.
4. **Idempotency Gateway Layer:** Header-based idempotency keys with SHA-256 payload verification and cached response replay.
5. **Transactional Event Streaming:** Reliable domain event publishing (`TransferInitiatedEvent`, `TransferSettledEvent`, `TransferFailedEvent`) using the Transactional Outbox Pattern.

### 3.2 Out-of-Scope (Future Phases)
- Physical card issuing and EMV chip profile lifecycle.
- Direct external clearing networks (e.g., SWIFT MT103/pacs.008, Fedwire, NPP, NAPAS).
- Complex interest accrual calculation engines and term deposits.

---

## 4. Functional Requirements

### Module 1: Customer & Account Lifecycle
- **FR-1.1 Customer Onboarding:** System must record verified customer identity data (Legal Name, Tax/National ID, Email, Phone Number, KYC Status).
- **FR-1.2 Account Provisioning:** A customer may hold multiple checking accounts in supported ISO 4217 currencies (`VND`, `AUD`, `USD`). Each account receives an immutable, unique account number.
- **FR-1.3 Account State Machine:**
  - `PENDING_KYC`: Initial state; debit and credit operations are blocked.
  - `ACTIVE`: Fully operational for credits and debits.
  - `FROZEN`: Administrative lock; credits are permitted, debits are rejected.
  - `CLOSED`: Terminal state; all further ledger entries are rejected.

### Module 2: Double-Entry Ledger
- **FR-2.1 Balance Derivation:** The account balance is mathematically defined as the aggregation of all historical ledger entries. The cached balance column on the account record serves strictly as a performance optimization and must reconcile to `Sum(Credits) - Sum(Debits)` at all times.
- **FR-2.2 Atomicity of Entries:** Every transfer must generate a minimum of two paired ledger entries within a single database transaction:
  - Exactly one `DEBIT` entry on the source account.
  - Exactly one `CREDIT` entry on the destination account.
  - Net transaction sum must equal zero: `Debit Amount == Credit Amount`.
- **FR-2.3 Ledger Immutability:** Ledger entries are strictly append-only. No application role or process may execute `UPDATE` or `DELETE` statements on `ledger_entries`.

### Module 3: Payment & Transfer Processing
- **FR-3.1 Validation Rules:** Prior to settlement, the engine must verify:
  - Source account state is `ACTIVE`.
  - Destination account state is `ACTIVE` or `FROZEN` (credits allowed).
  - Source account available balance satisfies `Available Balance >= Transfer Amount`.
  - Transfer amount is positive and within configured velocity limits.
- **FR-3.2 Transfer State Machine:**
  - `INITIATED` -> `PROCESSING` -> `SETTLED` (Successful completion).
  - `INITIATED` -> `PROCESSING` -> `FAILED` (Explicit failure with recorded reason).

### Module 4: Idempotency & API Reliability
- **FR-4.1 Mandatory Idempotency Key:** Mutating financial endpoints (`POST /api/v1/transfers`) require an `Idempotency-Key` HTTP header containing a unique UUID.
- **FR-4.2 Idempotency Semantics:**
  - **In-flight duplicate:** Returns `HTTP 409 Conflict` if an identical key is currently executing.
  - **Completed duplicate:** Returns the original cached response body and status code without re-executing business logic or modifying balances.
  - **Mismatched payload:** Returns `HTTP 422 Unprocessable Entity` if the same key is reused with a different request payload hash.

### Module 5: Transaction History & Statements
- **FR-5.1 Historical Querying:** Support range-based statement generation with pagination (keyset or offset).
- **FR-5.2 Running Balance:** Each statement line item must include the resulting account balance immediately following that specific transaction for customer reconciliation.

---

## 5. Non-Functional Requirements

- **NFR-1 Data Consistency (ACID):** Database transaction isolation must be configured at `READ COMMITTED` or higher, combined with deterministic row-level locks (`SELECT ... FOR UPDATE`) ordered by account identifier.
- **NFR-2 Strict Idempotency:** Zero duplicate ledger entries or balance deductions under network timeout, retry storms, or double-click scenarios.
- **NFR-3 Throughput & Latency:** 95th percentile latency (P95) for internal transfers must remain under 150ms under sustained loads of 200 Transactions Per Second (TPS).
- **NFR-4 Audit Compliance:** The system must maintain a permanent, non-repudiable audit log of all financial modifications.
- **NFR-5 Asynchronous Resilience:** Outbox pattern implementation guarantees at-least-once delivery to Apache Kafka. Upstream transfer commits must not fail if Kafka brokers are temporarily unavailable.

---

## 6. Milestone Schedule

```
Sprint 0: Infrastructure & Project Setup (Java 21, PostgreSQL 16, Flyway Migrations)
Sprint 1: Customer & Account Lifecycle (Entities, Validation, REST APIs)
Sprint 2: Atomic Transfer Engine & Double-Entry Ledger (Deadlock-Free Locking)
Sprint 3: API Reliability & Idempotency Layer (Header Filter & Replay)
Sprint 4: Event-Driven Architecture (Transactional Outbox + Kafka)
Sprint 5: Concurrency & Reliability Testing (Multi-Threaded Tests, Testcontainers)
Sprint 6: Production Hardening, Observability & Documentation
```
