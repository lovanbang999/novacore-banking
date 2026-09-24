# NovaCore Banking Engine: Account Bounded Context Structure

This document outlines the detailed folder and class hierarchy for the Account Bounded Context (Sprint 1), structured around Domain-Driven Design (DDD) and Hexagonal Architecture principles.

---

## 1. Directory Tree Hierarchy

```text
novacore-banking/src/main/java/com/novacore/banking/
├── NovacoreBankingApplication.java
│
├── account/                                # Account Bounded Context (Sprint 1)
│   ├── domain/                             # Core Business Models & Invariants (No Framework Dependencies)
│   │   ├── Account.java                    # Aggregate Root Entity (current_balance, status, version)
│   │   ├── Customer.java                   # Entity (full_name, email, kyc_status)
│   │   ├── AccountHold.java                # Entity (amount, reason, hold_status)
│   │   ├── AccountStatus.java              # Enum: PENDING_KYC, ACTIVE, FROZEN, CLOSED
│   │   ├── AccountType.java                # Enum: CHECKING, SAVINGS
│   │   ├── HoldStatus.java                 # Enum: ACTIVE, RELEASED, CAPTURED
│   │   ├── KycStatus.java                  # Enum: PENDING_KYC, VERIFIED, REJECTED
│   │   └── exception/                      # Domain Business Exceptions
│   │       ├── AccountNotFoundException.java
│   │       ├── CustomerNotFoundException.java
│   │       ├── KycNotVerifiedException.java
│   │       ├── IllegalStateTransitionException.java
│   │       └── AccountClosureException.java
│   │
│   ├── application/                        # Use Cases & Application Service Orchestration
│   │   ├── AccountService.java             # Service orchestrating account lifecycle & balances
│   │   └── dto/                            # Data Transfer Objects (Immutable Records)
│   │       ├── CreateAccountCommand.java   # Command DTO with Jakarta Bean Validation
│   │       └── AccountResponse.java        # Response DTO exposing current & available balances
│   │
│   └── infrastructure/                     # Outward Adapters (Spring Framework, DB, Web)
│       ├── persistence/                    # Database Persistence Layer (Spring Data JPA)
│       │   ├── AccountRepository.java      # JpaRepository for Account
│       │   ├── CustomerRepository.java     # JpaRepository for Customer
│       │   └── AccountHoldRepository.java  # JpaRepository for AccountHold (active holds query)
│       └── web/                            # Inbound Web Adapters (Spring MVC REST)
│           ├── AccountController.java      # REST Endpoints (/api/v1/accounts)
│           └── dto/
│               └── CreateAccountRequest.java # HTTP Request Body Schema
│
└── shared/                                 # Cross-Cutting Shared Modules
    └── exception/
        ├── GlobalExceptionHandler.java     # @RestControllerAdvice with RFC 7807 ProblemDetail
        └── ErrorResponse.java              # Standard Error Response Payload
```

---

## 2. Component Specifications & Invariants

### 2.1 Domain Layer (`com.novacore.banking.account.domain`)

#### Enums

1. **`AccountStatus.java`**
   - Values: `PENDING_KYC`, `ACTIVE`, `FROZEN`, `CLOSED`.
   - Transition Rules:
     - `PENDING_KYC` -> `ACTIVE` (only if customer KYC is verified).
     - `ACTIVE` -> `FROZEN`, `CLOSED`.
     - `FROZEN` -> `ACTIVE`, `CLOSED`.
     - `CLOSED` -> None (terminal state).

2. **`KycStatus.java`**
   - Values: `PENDING_KYC`, `VERIFIED`, `REJECTED`.

3. **`HoldStatus.java`**
   - Values: `ACTIVE`, `RELEASED`, `CAPTURED`.

4. **`AccountType.java`**
   - Values: `CHECKING`, `SAVINGS`.

#### Entities

1. **`Customer.java`**
   - Table: `customers`
   - Attributes:
     - `UUID id`: Primary Key.
     - `String fullName`: Customer full legal name.
     - `String email`: Unique constraint.
     - `String phoneNumber`: Unique constraint.
     - `KycStatus kycStatus`: Enum (`PENDING_KYC` by default).
     - `Instant createdAt`, `Instant updatedAt`.

2. **`Account.java` (Aggregate Root)**
   - Table: `accounts`
   - Attributes:
     - `UUID id`: Primary Key.
     - `UUID customerId`: Foreign Key referencing customer.
     - `String accountNumber`: Unique string identifier (30 chars max).
     - `String currency`: ISO 4217 code (`VND`, `USD`, `AUD`).
     - `AccountStatus status`: Initialized to `PENDING_KYC`.
     - `BigDecimal currentBalance`: `NUMERIC(19, 4)`, default `0.0000`.
     - `Long version`: `@Version` for optimistic concurrency locking.
     - `Instant createdAt`, `Instant updatedAt`.
   - Invariants & Encapsulated Operations:
     - `activate()`: Enforces valid state transition from `PENDING_KYC` or `FROZEN` to `ACTIVE`.
     - `freeze()`: Transitions `ACTIVE` to `FROZEN`.
     - `close()`: Enforces that `currentBalance == 0.0000` before transitioning to `CLOSED`.
     - `credit(BigDecimal amount)`: Increases `currentBalance`.
     - `debit(BigDecimal amount)`: Decreases `currentBalance` (fails if balance is insufficient).

3. **`AccountHold.java`**
   - Table: `account_holds`
   - Attributes:
     - `UUID id`: Primary Key.
     - `UUID accountId`: Foreign Key referencing account.
     - `BigDecimal amount`: Reserved amount (`NUMERIC(19, 4)`).
     - `String reason`: Purpose of hold (e.g. card pre-authorization).
     - `HoldStatus status`: Enum (`ACTIVE`, `RELEASED`, `CAPTURED`).
     - `Instant expiresAt`, `Instant createdAt`.
   - Invariants:
     - `release()`: Transitions from `ACTIVE` to `RELEASED`.
     - `capture()`: Transitions from `ACTIVE` to `CAPTURED`.

#### Domain Exceptions (`com.novacore.banking.account.domain.exception`)

- **`AccountNotFoundException`**: Thrown when account query by ID or account number returns empty.
- **`CustomerNotFoundException`**: Thrown when customer reference does not exist.
- **`KycNotVerifiedException`**: Thrown when account activation is attempted for an unverified customer.
- **`IllegalStateTransitionException`**: Thrown on illegal state transition (e.g. attempting to activate a `CLOSED` account).
- **`AccountClosureException`**: Thrown when closing an account with non-zero balance or active holds.

---

### 2.2 Application Layer (`com.novacore.banking.account.application`)

#### Data Transfer Objects (`com.novacore.banking.account.application.dto`)

1. **`CreateAccountCommand.java`** (Record)
   - `UUID customerId` (`@NotNull`)
   - `String currency` (`@NotBlank`, 3 chars)
   - `AccountType accountType` (`@NotNull`)

2. **`AccountResponse.java`** (Record)
   - `UUID id`
   - `String accountNumber`
   - `UUID customerId`
   - `String currency`
   - `AccountStatus status`
   - `BigDecimal currentBalance`
   - `BigDecimal availableBalance` (`currentBalance - SUM(active holds)`)
   - `BigDecimal activeHoldsAmount`

#### Application Service

**`AccountService.java`** (`@Service`, `@Transactional`)
- `AccountResponse createAccount(CreateAccountCommand command)`:
  - Verifies customer existence.
  - Generates unique account number.
  - Saves new account with `PENDING_KYC` status.
- `AccountResponse getAccountByNumber(String accountNumber)`:
  - Fetches account and calculates `availableBalance`.
- `AccountResponse activateAccount(String accountNumber)`:
  - Asserts customer KYC status is `VERIFIED`.
  - Executes `account.activate()`.
- `AccountResponse freezeAccount(String accountNumber)`:
  - Executes `account.freeze()`.
- `AccountResponse closeAccount(String accountNumber)`:
  - Asserts zero active holds and zero current balance.
  - Executes `account.close()`.

---

### 2.3 Infrastructure Layer (`com.novacore.banking.account.infrastructure`)

#### Persistence (`com.novacore.banking.account.infrastructure.persistence`)

1. **`CustomerRepository.java`**:
   - `Optional<Customer> findByEmail(String email)`
2. **`AccountRepository.java`**:
   - `Optional<Account> findByAccountNumber(String accountNumber)`
   - `boolean existsByAccountNumber(String accountNumber)`
3. **`AccountHoldRepository.java`**:
   - `List<AccountHold> findByAccountIdAndStatus(UUID accountId, HoldStatus status)`
   - Aggregate query for sum of active holds by `accountId`.

#### Web REST Controller (`com.novacore.banking.account.infrastructure.web`)

**`AccountController.java`** (`@RestController`, `@RequestMapping("/api/v1/accounts")`)
- `POST /api/v1/accounts`: Creates new account in `PENDING_KYC` status.
- `GET /api/v1/accounts/{accountNumber}`: Fetches balance and hold details.
- `PATCH /api/v1/accounts/{accountNumber}/activate`: Backoffice activation endpoint.
- `PATCH /api/v1/accounts/{accountNumber}/freeze`: Backoffice freeze endpoint.
- `PATCH /api/v1/accounts/{accountNumber}/close`: Backoffice close endpoint.

---

### 2.4 Shared Cross-Cutting Layer (`com.novacore.banking.shared.exception`)

**`GlobalExceptionHandler.java`** (`@RestControllerAdvice`)
Maps exceptions to RFC 7807 `ProblemDetail` or standard HTTP responses:
- `AccountNotFoundException`, `CustomerNotFoundException` -> HTTP 404 (Not Found).
- `KycNotVerifiedException` -> HTTP 422 (Unprocessable Entity).
- `IllegalStateTransitionException`, `AccountClosureException` -> HTTP 422 (Unprocessable Entity).
- `MethodArgumentNotValidException` -> HTTP 400 (Bad Request).

---

## 3. Implementation Order Checklist

- [ ] Step 1: Enums (`AccountStatus`, `KycStatus`, `HoldStatus`, `AccountType`)
- [ ] Step 2: Domain Exceptions (`domain/exception/*`)
- [ ] Step 3: Domain Entities (`Customer`, `AccountHold`, `Account`)
- [ ] Step 4: Spring Data Repositories (`CustomerRepository`, `AccountHoldRepository`, `AccountRepository`)
- [ ] Step 5: DTO Records (`CreateAccountCommand`, `AccountResponse`)
- [ ] Step 6: Application Service (`AccountService`)
- [ ] Step 7: Web Controller (`AccountController`) & Global Exception Handler (`GlobalExceptionHandler`)
- [ ] Step 8: Unit and Integration Tests
