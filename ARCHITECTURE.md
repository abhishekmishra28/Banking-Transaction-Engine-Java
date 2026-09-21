# System Architecture

The Banking Transaction Engine is built upon Clean Architecture and Domain-Driven Design (DDD) principles. The system is decoupled into four primary layers.

## 1. Domain Layer (`com.abhishek.banking.domain`)

The heart of the application. It contains no dependencies on external frameworks or databases.

*   **Models**: `Customer`, `Account` (abstract base), `SavingsAccount`, `CurrentAccount`, `Transaction`, `AuditLog`.
*   **Value Objects**: `Money` (encapsulates BigDecimal, ensuring a fixed scale and rounding mode for financial math).
*   **Exceptions**: Domain-specific exceptions like `InsufficientFundsException`, `InvalidAmountException`, `BankingException`.
*   **Rules**: 
    *   Accounts encapsulate their state and use a `ReentrantLock` to serialize local mutations (`deposit`, `withdraw`).
    *   Internal lock-free methods (`creditInternal`, `debitInternal`) are exposed strictly for cross-account transfers.

## 2. Application Layer (`com.abhishek.banking.application.service`)

Orchestrates domain objects to fulfill use cases.

*   **TransactionService**: The core orchestrator. 
    *   Implements **deadlock prevention**: When transferring money, it locks the two participating accounts in lexicographical order (based on Account ID). This ensures that if Thread A transfers from X to Y, and Thread B transfers from Y to X, they both attempt to lock the same account first, preventing a circular wait.
    *   Implements **rollback**: If a credit fails after a debit succeeds during a transfer, the debit is reversed before locks are released.
*   **AccountService / CustomerService**: Manage lifecycle and retrieval of entities.
*   **AuditService**: Records all business events asynchronously (conceptually).

## 3. Infrastructure Layer (`com.abhishek.banking.infrastructure.database` & `repository`)

Provides concrete implementations for persistence.

*   **DatabaseConnectionManager**: Manages JDBC connections to SQLite. Configures SQLite for high concurrency using `PRAGMA journal_mode = WAL`. Crucially, it manages a single shared `NonCloseableConnection` when running in `:memory:` mode to allow multiple repository calls to share the same transient database.
*   **Repositories**: Implementations like `AccountRepositoryImpl`, `TransactionRepositoryImpl` perform raw SQL queries and map `ResultSet` to Domain entities.

## 4. Presentation / Network Layer (`com.abhishek.banking.network`)

Exposes the application to the outside world via a custom TCP Server.

*   **BankingServer**: Listens on a configured port (e.g., 9090) and spawns a `ClientConnectionHandler` thread for each incoming connection.
*   **CommandParser**: Translates text-based protocol commands (e.g., `DEPOSIT ACC-123 50.00`) into calls against the Application services.
*   **TransactionEngine**: A thread-pool based queue for offloading heavy transaction processing, ensuring the network I/O threads aren't blocked by database/lock contention.
