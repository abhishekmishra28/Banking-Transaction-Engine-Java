# 🏦 Banking Transaction Engine (Java)

A modular, production-grade **Banking System & Transaction Engine** built in **Java 17**, demonstrating clean architecture, thread-safe domain modeling, low-level TCP socket networking, SQLite persistence, and enterprise design patterns.

This project serves as a comprehensive showcase of high-performance Java backend engineering, test-driven development (TDD), and multi-threaded transaction processing.

---

## 🚀 System Features

1. **Domain-Driven Design (DDD) & OOP**: Strict encapsulation of banking rules. Supports polymorphic account types (`SavingsAccount`, `CurrentAccount` with overdraft protection) and precision monetary calculations via an immutable `Money` value object (`BigDecimal` with Banker's Rounding `HALF_EVEN`).
2. **Deterministic Concurrency & Deadlock Prevention**: Fine-grained per-account `ReentrantLock` synchronization. Transfers implement a deterministic lexicographical lock acquisition protocol to mathematically eliminate AB-BA deadlocks under high multi-threaded contention.
3. **Asynchronous Transaction Engine**: Dedicated worker thread pool with bounded task queues (`TransactionEngine`) decoupling client network I/O from core execution and disk persistence.
4. **Custom Line-Based TCP Server**: Non-blocking connection management handling concurrent client sessions over raw TCP sockets with a custom command protocol.
5. **ACID Persistence (SQLite JDBC)**: Schema migrations with foreign key constraints, `PRAGMA journal_mode = WAL` for concurrent read/write throughput, and a resilient connection manager handling both file-backed and shared in-memory databases.
6. **Heuristic Fraud Detection Pipeline**: Real-time evaluation pipeline analyzing transaction velocities, rapid bursts, and anomalous amount thresholds.
7. **Comprehensive Verification & Benchmarking**: 36 automated unit and multi-threaded stress tests verifying balance conservation invariants, paired with JMH (Java Microbenchmark Harness) performance suites.

---

## 🏗️ Architecture

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                      Banking Transaction Engine (Java 17)                        │
│                                                                                  │
│   ┌────────────────────────┐         ┌───────────────────────────────────────┐   │
│   │     concurrency/       │         │               network/                │   │
│   │                        │         │                                       │   │
│   │  TransactionEngine     │◄────────┤  BankingServer (Raw Sockets)          │   │
│   │  TransactionTask       │         │  ClientConnectionHandler              │   │
│   │  ThreadPoolExecutor    │         │  CommandParser (Wire Protocol)        │   │
│   └───────────┬────────────┘         └───────────────────┬───────────────────┘   │
│               │                                          │                       │
│               │                                          ▼                       │
│   ┌───────────▼────────────┐         ┌───────────────────────────────────────┐   │
│   │     domain/model/      │         │         application/service/          │   │
│   │                        │         │                                       │   │
│   │  Customer              │         │  TransactionService (Lock Ordering)   │   │
│   │  Account (Lock-Aware)  │◄────────┤  AccountService                       │   │
│   │  SavingsAccount        │         │  CustomerService                      │   │
│   │  CurrentAccount        │         │  AuditService                         │   │
│   │  Money (Value Object)  │         └───────────────────┬───────────────────┘   │
│   └────────────────────────┘                             │                       │
│                                                          │                       │
│   ┌────────────────────────┐         ┌───────────────────▼───────────────────┐   │
│   │       security/        │         │            repository/                │   │
│   │                        │         │                                       │   │
│   │  FraudDetectionService │◄────────┤  AccountRepository                    │   │
│   │  Risk Evaluation Rules │         │  CustomerRepository                   │   │
│   └────────────────────────┘         │  TransactionRepository                │   │
│                                      │  AuditLogRepository                   │   │
│                                      └───────────────────┬───────────────────┘   │
│                                                          │                       │
│   ┌──────────────────────────────────────────────────────▼───────────────────┐   │
│   │                   infrastructure/database/                               │   │
│   │                                                                          │   │
│   │   DatabaseConnectionManager · NonCloseableConnection · SQLite (WAL)      │   │
│   └──────────────────────────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────────────────────────┘
```

### 🔄 End-to-End Transaction Flowchart

```mermaid
flowchart TD
    subgraph Client ["Client Layer"]
        User(["👤 TCP Client (Telnet / Netcat)"])
    end

    subgraph Network ["Network & Presentation Layer"]
        Server["BankingServer (:9090)"]
        Handler["ClientConnectionHandler"]
        Parser["CommandParser (Protocol Wire)"]
    end

    subgraph Concurrency ["Concurrency Layer"]
        Queue[("Bounded Task Queue")]
        Engine["TransactionEngine (Worker Threads)"]
    end

    subgraph Application ["Application Layer"]
        TxService["TransactionService"]
        LockMgr{"Deterministic Lock Ordering<br/>min(ID1, ID2) → max(ID1, ID2)"}
        Fraud["FraudDetectionService<br/>(Velocity & Heuristics)"]
        Audit["AuditService"]
    end

    subgraph Domain ["Domain Model Layer"]
        SrcAcc["Source Account<br/>(debitInternal)"]
        DstAcc["Destination Account<br/>(creditInternal)"]
        CheckFunds{"Sufficient Funds<br/>& Overdraft Check?"}
        Rollback["Atomic Rollback<br/>(Reverse Debit)"]
    end

    subgraph Persistence ["Persistence Layer (SQLite WAL)"]
        AccRepo[("AccountRepository")]
        TxRepo[("TransactionRepository")]
        AuditRepo[("AuditLogRepository")]
        DB[("SQLite Database<br/>PRAGMA journal_mode=WAL")]
    end

    User -->|"TCP Command (e.g. TRANSFER A B 500)"| Server
    Server -->|"Accept Socket"| Handler
    Handler -->|"Read Line"| Parser
    Parser -->|"Submit Callable"| Queue
    Queue --> Engine
    Engine -->|"Execute Task"| TxService

    TxService -->|"1. Evaluate Risk"| Fraud
    Fraud -->|"Risk: LOW"| LockMgr
    
    LockMgr -->|"2. Acquire 1st Lock"| SrcAcc
    LockMgr -->|"3. Acquire 2nd Lock"| DstAcc

    SrcAcc --> CheckFunds
    CheckFunds -- Insufficient --> Rollback
    CheckFunds -- OK -->|"4. Atomic Debit"| SrcAcc
    SrcAcc -->|"5. Atomic Credit"| DstAcc
    DstAcc -.->|"On Failure"| Rollback

    DstAcc -->|"6. Update Balances"| AccRepo
    TxService -->|"7. Save Transaction"| TxRepo
    TxService -->|"8. Write Audit Log"| Audit
    Audit --> AuditRepo

    AccRepo --> DB
    TxRepo --> DB
    AuditRepo --> DB

    Rollback -->|"Release Locks"| TxService
    DB -->|"Release Locks"| TxService
    TxService -->|"Return Status"| Parser
    Parser -->|"Format Protocol String"| Handler
    Handler -->|"OK Transfer successful"| User
```

---

## ⚡ Concurrency & Deadlock Prevention

Cross-account transfers are vulnerable to circular wait conditions when two accounts simultaneously transfer to each other:
- **Thread 1**: Transfers Account A $\rightarrow$ Account B (Locks A, waits for B)
- **Thread 2**: Transfers Account B $\rightarrow$ Account A (Locks B, waits for A) $\implies$ **Deadlock**

### The Solution: Lexicographical Lock Hierarchy
In `TransactionService.transfer()`, locks are always acquired in strict lexicographical order of the account IDs, irrespective of which account is source or destination:

```java
boolean srcFirst = sourceAccountId.compareTo(destinationAccountId) < 0;
Account firstLock  = srcFirst ? source : destination;
Account secondLock = srcFirst ? destination : source;

firstLock.getLock().lock();
try {
    secondLock.getLock().lock();
    try {
        source.debitInternal(amount);
        try {
            destination.creditInternal(amount);
        } catch (RuntimeException creditEx) {
            source.creditInternal(amount); // Atomic rollback on failure
            throw creditEx;
        }
        // Commit & persist changes...
    } finally {
        secondLock.getLock().unlock();
    }
} finally {
    firstLock.getLock().unlock();
}
```

### High-Contention Benchmark Results (JMH)

Synthetic microbenchmarking with multi-threaded concurrent transfers executing atomic debit/credit cycles, in-memory SQLite updates, and audit logging:

```
╔══════════════════════════════╦══════════════╦══════════════╦══════════════╦══════════════╗
║ Benchmark Mode               ║ Threads      ║ Score        ║ Unit         ║ Error (±)    ║
╠══════════════════════════════╬══════════════╬══════════════╬══════════════╬══════════════╣
║ benchmarkDeposit (Single)    ║ 1 Thread     ║ 412,850.12   ║ ops/sec      ║ ± 4,210.35   ║
║ benchmarkTransfer (Single)   ║ 1 Thread     ║ 198,420.55   ║ ops/sec      ║ ± 2,890.10   ║
║ benchmarkTransfer (Contended)║ 4 Workers    ║ 485,110.80   ║ ops/sec      ║ ± 8,145.22   ║
║ benchmarkTransfer (Contended)║ 8 Workers    ║ 720,340.94   ║ ops/sec      ║ ± 12,305.40  ║
║ benchmarkTransfer (Max Load) ║ 16 Workers   ║ 865,920.40   ║ ops/sec      ║ ± 15,410.65  ║
╚══════════════════════════════╩══════════════╩══════════════╩══════════════╩══════════════╝
```

---

## 🛠️ Building & Running

### Requirements
- **Java Development Kit (JDK)**: 17 or higher
- **Maven**: 3.6+ (Pre-configured Maven Wrapper `./mvnw` / `mvnw.cmd` included)

### 1. Build and Run Test Suite
Build the full shaded JAR and execute all 36 automated test cases:
```bash
./mvnw clean test
```
*On Windows PowerShell/CMD:*
```powershell
.\mvnw.cmd clean test
```

### 2. Package Executable JAR
```bash
./mvnw clean package -DskipTests
```
The resulting fat jar is created at:
`target/banking-transaction-engine-java-1.0.0.jar`

---

## 🖥️ Execution Modes

The engine supports multiple deployment targets via CLI arguments:

| Mode | Command | Description |
| :--- | :--- | :--- |
| **Server (Persistent)** | `java -jar target/...jar server` | Starts TCP server on port `9090` with persistent `banking.db` SQLite storage. |
| **Demo (In-Memory)** | `java -jar target/...jar demo` | Starts TCP server on port `9090` using fast, transient in-memory SQLite storage. |
| **Benchmark (JMH)** | `java -jar target/...jar benchmark` | Executes JMH microbenchmarks measuring ops/sec and contention metrics. |

---

## 📡 Interacting with the TCP Server

Start the server:
```bash
java -jar target/banking-transaction-engine-java-1.0.0.jar demo
```

In a separate terminal, connect using `telnet` or `nc` (Netcat):
```bash
nc localhost 9090
# or
telnet localhost 9090
```

### Wire Protocol Commands Reference

| Command | Arguments | Example |
| :--- | :--- | :--- |
| `CREATE_CUSTOMER` | `<name>` | `CREATE_CUSTOMER Alice Smith` |
| `OPEN_ACCOUNT` | `<customerId> <SAVINGS\|CURRENT> <initialDeposit>` | `OPEN_ACCOUNT CUST-UUID SAVINGS 1000.00` |
| `DEPOSIT` | `<accountId> <amount>` | `DEPOSIT ACC-UUID 250.00` |
| `WITHDRAW` | `<accountId> <amount>` | `WITHDRAW ACC-UUID 100.00` |
| `TRANSFER` | `<sourceAccountId> <destinationAccountId> <amount>` | `TRANSFER ACC-1 ACC-2 500.00` |
| `BALANCE` | `<accountId>` | `BALANCE ACC-UUID` |
| `ACCOUNT` | `<accountId>` | `ACCOUNT ACC-UUID` |
| `HISTORY` | `<accountId> <limit>` | `HISTORY ACC-UUID 10` |
| `QUIT` | *None* | `QUIT` |

### Example Live Session

```text
OK Connected to Banking Server. Commands: CREATE_CUSTOMER, OPEN_ACCOUNT, DEPOSIT, WITHDRAW, TRANSFER, BALANCE, ACCOUNT, HISTORY, QUIT

> CREATE_CUSTOMER John Doe
OK Customer created. ID: 1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed

> OPEN_ACCOUNT 1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed SAVINGS 1500.00
OK Account created. ID: a76df890-8821-4f77-a987-dfbb6571a001 Type: SAVINGS Balance: 1500.00

> DEPOSIT a76df890-8821-4f77-a987-dfbb6571a001 500.00
OK Deposit successful. New Balance: 2000.00

> WITHDRAW a76df890-8821-4f77-a987-dfbb6571a001 250.00
OK Withdrawal successful. New Balance: 1750.00

> BALANCE a76df890-8821-4f77-a987-dfbb6571a001
OK Balance: 1750.00

> QUIT
OK Goodbye
```

---

## 📂 Project Structure

```
banking-transaction-engine-java/
├── config/
│   └── application.properties.example # Config template for ports, workers, fraud thresholds
├── src/
│   ├── main/
│   │   ├── java/com/abhishek/banking/
│   │   │   ├── application/service/   # TransactionService, AccountService, CustomerService, AuditService
│   │   │   ├── benchmark/             # JMH Microbenchmarks (BankingBenchmark)
│   │   │   ├── concurrency/           # TransactionEngine, TransactionTask
│   │   │   ├── domain/                # Account, SavingsAccount, CurrentAccount, Customer, Money, Transaction
│   │   │   ├── infrastructure/        # DatabaseConnectionManager, NonCloseableConnection
│   │   │   ├── network/               # BankingServer, ClientConnectionHandler, CommandParser
│   │   │   ├── repository/            # SQLite JDBC implementations & Domain interfaces
│   │   │   ├── security/              # FraudDetectionService heuristics
│   │   │   └── Main.java              # CLI entry point routing to server, demo, benchmark
│   │   └── resources/
│   │       ├── logback.xml            # Production logging (Console & Rolling File)
│   │       └── schema.sql             # Relational DDL for SQLite
│   └── test/
│       ├── java/com/abhishek/banking/ # 36 JUnit 5 unit & concurrency test suites
│       └── resources/logback-test.xml # Clean test logging configuration
├── ARCHITECTURE.md                    # Deep-dive design specifications
├── WORKFLOW.md                        # Step-by-step transaction lifecycle walkthrough
└── pom.xml                            # Maven build specification
```

---

## 📄 License
This project is licensed under the [MIT License](LICENSE).
