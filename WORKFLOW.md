# Transaction Workflow

This document traces the lifecycle of a `TRANSFER` command through the system to illustrate how the components interact.

## 1. Network Reception
1. A client sends a string over TCP: `TRANSFER ACC-111 ACC-222 500.00`
2. `BankingServer` accepts the connection and hands the socket to a `ClientConnectionHandler`.
3. The handler reads the line and passes it to the `CommandParser`.

## 2. Command Parsing
1. `CommandParser` splits the string, validates the format, and parses the `500.00` into a `Money` value object.
2. It constructs a `Callable<String>` task that will execute the transfer.
3. The task is submitted to the `TransactionEngine` (a bounded thread pool).

## 3. Orchestration & Concurrency Control
1. A worker thread from the `TransactionEngine` picks up the task and calls `TransactionService.transfer(ACC-111, ACC-222, 500.00)`.
2. `TransactionService` asks `AccountService` for the `Account` entities for both IDs.
3. `AccountService` fetches the latest state from the SQLite database via `AccountRepository`.
4. **Lock Acquisition**: `TransactionService` determines the lock order. Since "ACC-111" < "ACC-222", it calls `acc111.getLock().lock()` followed by `acc222.getLock().lock()`.

## 4. Domain Execution & Rollback
1. With both locks held, `TransactionService` calls the internal, non-locking methods on the domain objects:
    * `acc111.debitInternal(500.00)`
    * `acc222.creditInternal(500.00)`
2. If `creditInternal` throws an exception, `TransactionService` catches it, reverses the debit via `acc111.creditInternal(500.00)`, and re-throws.

## 5. Persistence
1. If the domain updates succeed, `TransactionService` calls `AccountService.updateAccount()` for both accounts.
2. A `Transaction` record is created with status `COMPLETED`.
3. `AuditService` logs the event.
4. **Lock Release**: The locks for ACC-222 and ACC-111 are released in a `finally` block.
5. The `Transaction` record is persisted to the database.

## 6. Response
1. The `TransactionService` returns the completed `Transaction` object.
2. The `CommandParser` formats this into a string: `OK Transfer successful. TX ID: TX-XYZ`
3. `ClientConnectionHandler` writes the response back to the TCP socket.
