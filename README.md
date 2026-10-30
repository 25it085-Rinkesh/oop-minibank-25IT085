# MiniBank (`oop-minibank-25IT085`)

A comprehensive, production-grade Object-Oriented Banking Application developed in Java. MiniBank integrates foundational OOP principles, polymorphism, pattern matching, custom checked exceptions, Java reflection, thread synchronization, executor service concurrency, Java object serialization, NIO-based append-only logging and report generation, Java Collections, and a reflection-based generic repository into a unified, interactive banking console application.

---

## 1. Project Purpose & Overview

MiniBank simulates an enterprise core-banking engine. It provides reliable account lifecycle management, transaction processing (deposits, withdrawals, transfers), audit logging, deadlock-free concurrency, asynchronous batch operations, object persistence across restarts, and end-of-day reconciliation reports.

---

## 2. Java Version & Prerequisites

- **Java Development Kit (JDK)**: Java 17 or higher (tested with JDK 21 / Oracle JDK 27).
- **Build Tools**: Standard JDK binaries (`javac`, `jar`, `java`). No external package managers required.
- **Operating System**: Platform independent (Windows, macOS, Linux).

---

## 3. Project & Package Structure

```text
oop-minibank-25IT085/
├── src/
│   ├── exception/                  # Custom checked banking exceptions
│   │   ├── BankException.java
│   │   ├── AccountNotFoundException.java
│   │   ├── InsufficientFundsException.java
│   │   └── InvalidAmountException.java
│   ├── model/                      # Core domain entities and interfaces
│   │   ├── Account.java            # Abstract base account (Serializable, Comparable)
│   │   ├── SavingsAccount.java     # Savings account with minimum-balance enforcement
│   │   ├── CurrentAccount.java     # Current account with overdraft-limit protection
│   │   ├── FixedDepositAccount.java# Locked deposit account (7% interest, withdrawals rejected)
│   │   ├── Customer.java           # Customer record with Address and cloning support
│   │   ├── BankInfo.java           # Immutable record for branch information
│   │   ├── Command.java            # Parsed command record
│   │   ├── InterestBearing.java    # Interface providing yearly interest computation
│   │   ├── Transactable.java       # Interface defining core deposit/withdraw contract
│   │   ├── Premium.java            # Marker interface
│   │   ├── MenuOption.java         # Interactive menu enum (options 1-9)
│   │   ├── TransactionType.java    # DEPOSIT, WITHDRAW, TRANSFER enum
│   │   └── annotation/             # Custom runtime annotations
│   │       ├── Audit.java          # Operational audit annotation for reflection
│   │       ├── Id.java             # Entity primary key identifier
│   │       ├── MaxLength.java      # String length validation constraint
│   │       └── Positive.java       # Positive numeric constraint
│   ├── repository/                 # Generic reflection-backed storage
│   │   └── Repository.java         # Generic Repository<T> with hierarchy @Id discovery
│   ├── service/                    # Business services and concurrency engine
│   │   ├── BankService.java        # Core banking service API
│   │   ├── AccountStore.java       # HashMap account store with Comparator/Comparable sorting
│   │   ├── AccountTransactionTask.java # Runnable task for executor thread pool
│   │   ├── AccountWorker.java      # Concurrency worker demonstrating synchronization
│   │   ├── BatchTransaction.java   # Immutable batch transaction instruction
│   │   ├── BatchResult.java        # Thread-safe batch execution summary
│   │   ├── DemoResource.java       # AutoCloseable resource for try-with-resources demo
│   │   ├── MiniBank.java           # Main application entry point and interactive console
│   │   ├── TransactionBuffer.java  # Bounded circular wait/notify producer-consumer buffer
│   │   ├── TransactionProcessor.java # 4-thread ExecutorService with ConcurrentHashMap registry
│   │   └── WithdrawRule.java       # Functional interface for withdrawal rules
│   └── util/                       # Persistence, logging, validation, and reporting utilities
│       ├── AnnotationValidator.java# Reflection validator for @Positive and @MaxLength
│       ├── CommandParser.java      # Command string parser
│       ├── ReportGenerator.java    # NIO directory-stream log aggregator & report builder
│       ├── StatementFormatter.java # Account statement formatter
│       ├── StatePersister.java     # ObjectInputStream / ObjectOutputStream persistence
│       ├── TransactionLog.java     # UTF-8 append-only transaction logger
│       └── Validator.java          # Regex validation (Mobile, Email, PAN, IFSC)
├── test/                           # JUnit 5 test suites
│   ├── TestRunner.java             # CLI test runner using JUnit Platform Launcher
│   └── service/
│       └── BankServiceTest.java    # Core unit and integration test suite
├── lib/
│   └── test/                       # JUnit 5 test libraries (Jupiter API, Engine, Platform)
├── META-INF/
│   └── MANIFEST.MF                 # Runnable JAR manifest (Main-Class: service.MiniBank)
├── dist/
│   └── MiniBank.jar                # Packaged runnable JAR
└── data/                           # Runtime persistence, logs, and reports
    ├── accounts.dat                # Serialized Account state
    ├── audit/                      # Operational audit logs
    │   └── audit.log
    ├── logs/                       # Daily append-only transaction logs
    │   └── transactions-*.log
    └── reports/                    # Generated End-of-Day reconciliation reports
        └── end-of-day-report.txt
```

---

## 4. Features & Supported Account Types

MiniBank supports three concrete account types under `model.Account`:

1. **SavingsAccount**:
   - Offers 4.0% annual interest rate.
   - Requires maintaining a non-negative minimum balance (`minBalance`).
   - Rejects withdrawals or transfers that would leave the balance below `minBalance`.
2. **CurrentAccount**:
   - Offers 0.0% interest rate (commercial operational account).
   - Permits overdraft up to a specified non-negative limit (`overdraftLimit`).
   - Balance can dip to `-overdraftLimit`.
3. **FixedDepositAccount**:
   - Offers 7.0% annual interest rate.
   - Fully locked against withdrawals (`maxWithdrawable() == 0`). Any withdrawal attempt throws `InsufficientFundsException`.

---

## 5. Banking Menu Operations

When launched, MiniBank presents an interactive 9-option menu:

```text
---------- Menu ----------
1. Open Account
2. Deposit
3. Withdraw
4. Transfer
5. View Account Statement
6. List All Accounts
7. Generate End-of-Day Report
8. Process Batch Transactions
9. Exit
```

- **1. Open Account**: Prompts for account type (Savings, Current, Fixed Deposit), customer name, initial deposit, and type-specific rules (minimum balance or overdraft limit). Validates fields via annotations before storing.
- **2. Deposit**: Credits a positive amount to the account, enforcing overflow protection and recording the transaction.
- **3. Withdraw**: Debits an account subject to account-specific rules, preventing underflow and insufficient funds.
- **4. Transfer**: Atomically moves funds between two accounts using deterministic lock ordering to guarantee deadlock prevention and automatic rollback if the credit step fails.
- **5. View Account Statement**: Formats an official account statement including current balance and full transaction history extracted from daily logs.
- **6. List All Accounts**: Displays a tabular overview of all accounts in the repository.
- **7. Generate End-of-Day Report**: Invokes the NIO report generator to aggregate daily deposits, withdrawals, net movement, and file metadata across all log files.
- **8. Process Batch Transactions**: Submits a batch of concurrent transactions to the 4-thread `TransactionProcessor`, coordinating execution via `CountDownLatch` and returning a thread-safe summary.
- **9. Exit**: Flushes pending batch jobs, shuts down the executor pool, persists all accounts to `data/accounts.dat`, and displays a polite goodbye message.

---

## 6. Account Validation & Exceptions

MiniBank enforces strict numeric and business safety:
- **`@Positive` & `@MaxLength`**: Checked reflectively by `AnnotationValidator.validate()`. Rejects invalid balance entries or names exceeding 50 characters.
- **`InvalidAmountException`**: Checked exception thrown when amounts are zero, negative, or would cause 64-bit integer overflow.
- **`InsufficientFundsException`**: Checked exception carrying the exact non-negative `shortfall` amount when an account cannot cover a withdrawal or transfer.
- **`AccountNotFoundException`**: Thrown when a targeted account ID does not exist.
- **Deadlock-Free Transfer**: `Account.transfer()` compares account IDs lexicographically (`compareTo`) and acquires locks in consistent order (`synchronized (first) { synchronized (second) { ... } }`). If credit fails, rollback credits the source back and attaches rollback issues as suppressed exceptions (`addSuppressed`).

---

## 7. Repository & Persistence Implementation

- **Generic `Repository<T>`**:
  - Implements a generic storage model using `HashMap<String, T>`.
  - Discovers the entity's identifier by reflectively inspecting declared fields across the entire class hierarchy for the `@Id` annotation.
  - Exposes `save()`, `findById()`, `findAll()` (defensive snapshot), `delete()`, `saveAll()` (with `@SafeVarargs`), and a bounded static generic method `<N extends Number> double average(N[] values)`.
- **State Persistence (`StatePersister`)**:
  - Uses `ObjectOutputStream` and `ObjectInputStream` to serialize/deserialize `Account[]` arrays to `data/accounts.dat`.
  - Handles auto-recovery of `accountCounter` during deserialization (`readObject`) to prevent ID collisions with existing accounts.

---

## 8. Transaction Processing & Thread Safety

- **`TransactionProcessor`**:
  - Hosts a fixed thread pool of 4 worker threads (`Executors.newFixedThreadPool(4)`).
  - Maintains a thread-safe `ConcurrentHashMap<String, Account>` registry ensuring single-source-of-truth account resolution across threads.
  - Implements bounded two-stage shutdown (`shutdown()` &rarr; 10s timeout &rarr; `shutdownNow()`).
- **Batch Processing**:
  - Submits concurrent `BatchTransaction` tasks and synchronizes completion using `CountDownLatch`.
  - Collects success counts, failure counts, and thread-safe error logs without busy-waiting.

---

## 9. How to Run JUnit Tests

MiniBank includes a comprehensive JUnit 5 test suite in `test/service/BankServiceTest.java`.

### Run tests using PowerShell:
```powershell
# 1. Compile production sources into build/
New-Item -ItemType Directory -Force .\build | Out-Null
$sourceFiles = Get-ChildItem .\src -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -d .\build $sourceFiles
if ($LASTEXITCODE -ne 0) { throw "Production compilation failed." }

# 2. Compile tests into out/test/
New-Item -ItemType Directory -Force .\out\test | Out-Null
$testFiles = Get-ChildItem .\test -Recurse -Filter "*.java" | Select-Object -ExpandProperty FullName
javac -encoding UTF-8 -cp "lib/test/*;build" -d .\out\test $testFiles
if ($LASTEXITCODE -ne 0) { throw "Test compilation failed." }

# 3. Execute the JUnit 5 test runner
java -cp "lib/test/*;build;out/test" test.TestRunner
```

### Verified Test Cases (24 Passed):
1. `testDepositIncreasesBalance`: Validates balance increases correctly upon deposit.
2. `testWithdrawalBeyondAllowedLimit`: Validates `InsufficientFundsException` and shortfall when exceeding limits.
3. `testConcurrentDeposits`: Concurrently executes 100 deposits of ₹25 each on one account, verifying the exact final balance.
4. `testAccountNotFound`: Confirms `AccountNotFoundException` on missing IDs.
5. `testInvalidDepositAmount`: Verifies rejection of zero and negative amounts.
6. `testSuccessfulTransfer`: Verifies atomic debit and credit across accounts.
7. `testFailedTransferLeavesBalancesUnchanged`: Confirms rollback and balance preservation on failed transfers.
8. `testAccountPersistenceAndReload`: Tests complete serialization round-trip and subclass field preservation.
9. `testRepositoryLookupAfterAccountCreation`: Confirms immediate repository discovery and object reference identity.
10. `testMonotonicAccountSequenceRecovery`: Validates durable, monotonic recovery from existing logs and sequence tracking files across JVM restarts.
11. `testAccountStatementContainsOnlyTargetAccountTransactions`: Confirms complete isolation of account statements, preventing cross-account transaction history contamination.
12. `testAuditLogIsolationAndProductionImmutability`: Proves test audit records are isolated to temporary directories and production `data/audit/audit.log` remains strictly immutable (SHA-256 verified).
13. `testSuccessfulBatchProcessing`: Verifies concurrent batch deposit and withdrawal execution with accurate final balances.
14. `testBatchContainingFailedTransactions`: Asserts worker exceptions (e.g. overdraft limit breach, missing account) are captured individually without aborting the batch.
15. `testBatchExecutorRejection`: Verifies stopped/saturated executor cleanly marks tasks as rejected rather than hanging or claiming false success.
16. `testBatchInterruptionHandling`: Confirms interrupt status is properly preserved on the caller thread upon batch completion.
17. `testEmptyAndNullBatch`: Validates graceful zero-count return for empty and null batch lists.
18. `testBatchAccurateFinalCounts`: Validates core accounting invariants: `total = accepted + rejected`, `completed = succeeded + failed`, and `accepted == completed`.
19. `testRepositoryAndRegistryReferenceIdentity`: Proves `Repository` and `TransactionProcessor` resolve the exact same object reference (`assertSame`).
20. `testDuplicateAccountRegistrationRejected`: Ensures registering an already-registered account into the processor throws `IllegalStateException`.
21. `testRestoredAccountsAvailableToBatchProcessing`: Confirms deserialized accounts loaded into repository and registry are immediately accessible for concurrent batch execution.
22. `testAccountStatementChronologicalSortingMultipleFiles`: Validates chronological statement ordering across multiple distinct daily log files.
23. `testTransactionLogWriteErrorPropagatedHonestly`: Proves I/O logging errors throw `TransactionLoggingException` while preserving successful financial state changes without silent swallowing or duplicate execution.
24. `testComprehensiveSequenceRecoveryAcrossAllSources`: Validates monotonic sequence discovery across historical logs, serialized state, and durable sequence tracker without touching production files.

---

## 10. How to Build the Runnable JAR

To package the entire production application into `dist/MiniBank.jar` on a fresh clone without relying on untracked `META-INF` directories:

```powershell
# 1. Clean previous build artifacts and ensure target directories exist
Remove-Item -Recurse -Force .\build -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force .\build, .\dist | Out-Null

# 2. Compile all production Java sources
$sourceFiles = Get-ChildItem .\src -Recurse -Filter "*.java" |
    Select-Object -ExpandProperty FullName

javac -encoding UTF-8 -d .\build $sourceFiles

if ($LASTEXITCODE -ne 0) {
    throw "Production compilation failed."
}

# 3. Create runnable JAR with automatic Main-Class manifest generation
jar --create --file .\dist\MiniBank.jar `
    --main-class service.MiniBank -C .\build .
```

---

## 11. How to Run the Application

### A. Normal Interactive Banking Mode (Default)
Loads persisted accounts from `data/accounts.dat`, recovers account sequence monotonically from transaction logs and storage, and immediately displays the interactive banking console:

```powershell
java -jar dist\MiniBank.jar
```

Or run via compiled class files:
```powershell
java -cp build service.MiniBank
```

### B. Educational Demonstration Mode (`--demo`)
Runs all Practical 1–13 educational demonstrations (polymorphism, pattern matching, custom exceptions, synchronization, producer-consumer, deadlock prevention, persistence, NIO reports, collections, and generic reflection repository). All demonstration files are strictly isolated to `data/demo/`, leaving production logs and accounts untouched:

```powershell
java -jar dist\MiniBank.jar --demo
```

---

## 12. Storage & Log Locations

All application runtime data is stored cleanly inside the `data/` directory:
- **`data/accounts.dat`**: Serialized binary account store containing all persisted account instances.
- **`data/sequence.dat`**: Durable sequence tracker storing the highest allocated account ID to guarantee monotonic numbering across sessions.
- **`data/logs/transactions-YYYY-MM-DD.log`**: Append-only daily transaction records in UTF-8 format (`timestamp|accountNumber|type|amount`).
- **`data/reports/end-of-day-report.txt`**: Aggregated end-of-day reconciliation report generated via Java NIO.
- **`data/audit/audit.log`**: Operational audit trail generated by reflective `@Audit` annotations.
- **`data/demo/`**: Isolated sandbox storage directory for educational demonstrations (`--demo`), preventing collision with production accounts and transaction logs.

---

## 13. Sample Interaction Guide

```text
==================================
       WELCOME TO MINIBANK
==================================
Bank Name : MiniBank
Branch    : Navsari
Restored 3 accounts from data\accounts.dat

---------- Menu ----------
1. Open Account
2. Deposit
3. Withdraw
4. Transfer
5. View Account Statement
6. List All Accounts
7. Generate End-of-Day Report
8. Process Batch Transactions
9. Exit
Enter your choice: 1

--- Open New Account ---
Select Account Type:
  1. Savings Account
  2. Current Account
  3. Fixed Deposit Account
Enter account type (1-3): 1
Enter Owner Name: Rajesh Sharma
Enter Opening Balance (₹): 10000
Enter Minimum Balance (₹): 1000
Account opened successfully! Account Number: AC0045 | Balance: ₹10000

---------- Menu ----------
Enter your choice: 2

--- Deposit Funds ---
Enter Account Number: AC0045
Enter Deposit Amount (₹): 5000
Deposit of ₹5000 successful. New balance: ₹15000

---------- Menu ----------
Enter your choice: 5

--- View Account Statement ---
Enter Account Number: AC0045

========== ACCOUNT STATEMENT ==========
Account Number : AC0045
Owner Name     : Rajesh Sharma
Balance        : ₹15000
Account Status : Active
=======================================

TRANSACTION HISTORY
------------------------------------------------------------
Date / Time (UTC)       Type        Amount
------------------------------------------------------------
2026-10-30T16:38:19Z    DEPOSIT     ₹10000
2026-10-30T16:40:02Z    DEPOSIT     ₹5000
============================================================

---------- Menu ----------
Enter your choice: 9
Saved 4 accounts to data\accounts.dat
Thank you for using MiniBank. Goodbye!
```
