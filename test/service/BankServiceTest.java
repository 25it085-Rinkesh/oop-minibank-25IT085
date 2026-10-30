package test.service;

import exception.AccountNotFoundException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import exception.TransactionLoggingException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import repository.Repository;
import service.BankService;
import service.BatchResult;
import service.BatchTransaction;
import service.TransactionProcessor;
import util.StatePersister;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive JUnit 5 test suite for BankService, thread coordination,
 * audit isolation, account sequence recovery, and data integrity.
 */
public class BankServiceTest {

    private Repository<Account>  repository;
    private TransactionProcessor processor;
    private BankService          bankService;
    private Path                 tempBaseDir;
    private Path                 tempLogsDir;
    private Path                 tempSeqFile;
    private Path                 tempAuditFile;

    @BeforeEach
    void setUp() throws IOException {
        repository    = new Repository<>();
        processor     = new TransactionProcessor();
        tempBaseDir   = Files.createTempDirectory("minibank-test-root");
        tempLogsDir   = tempBaseDir.resolve("logs");
        tempSeqFile   = tempBaseDir.resolve("sequence.dat");
        tempAuditFile = tempBaseDir.resolve("audit").resolve("audit.log");

        Files.createDirectories(tempLogsDir);
        Files.createDirectories(tempAuditFile.getParent());

        bankService   = new BankService(repository, processor, tempLogsDir, tempSeqFile, tempAuditFile);
    }

    @AfterEach
    void tearDown() throws IOException {
        processor.stop();
        if (tempBaseDir != null && Files.exists(tempBaseDir)) {
            try (var stream = Files.walk(tempBaseDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                      .forEach(p -> {
                          try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                      });
            }
        }
    }

    private static String computeSha256(Path file) throws Exception {
        if (!Files.exists(file)) {
            return "NON_EXISTENT";
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] bytes = Files.readAllBytes(file);
        byte[] hash = digest.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // Test 1 — Deposit increases balance
    // -------------------------------------------------------------------------
    @Test
    void testDepositIncreasesBalance() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("Alice", 5_000, 1_000);
        long initialBalance = acc.getBalance();

        bankService.deposit(acc.getAccountNumber(), 2_500);

        assertEquals(initialBalance + 2_500, acc.getBalance(),
                "Deposit must increase the balance by the exact amount.");
    }

    // -------------------------------------------------------------------------
    // Test 2 — Withdrawal beyond the allowed limit
    // -------------------------------------------------------------------------
    @Test
    void testWithdrawalBeyondAllowedLimit() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("Bob", 3_000, 1_000);
        long balanceBefore = acc.getBalance();

        InsufficientFundsException ex = assertThrows(
                InsufficientFundsException.class,
                () -> bankService.withdraw(acc.getAccountNumber(), 2_500),
                "Withdrawing beyond permitted limit must throw InsufficientFundsException."
        );

        assertEquals(500, ex.getShortfall(), "Shortfall must equal requested - maxWithdrawable.");
        assertEquals(balanceBefore, acc.getBalance(), "Account balance must remain unchanged on rejected withdrawal.");
    }

    // -------------------------------------------------------------------------
    // Test 3 — Concurrent deposits
    // -------------------------------------------------------------------------
    @Test
    void testConcurrentDeposits() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("Charlie", 1_000, 0);
        final int TASK_COUNT = 100;
        final long DEPOSIT_AMT = 25L;

        List<BatchTransaction> batch = new ArrayList<>();
        for (int i = 0; i < TASK_COUNT; i++) {
            batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.DEPOSIT, DEPOSIT_AMT));
        }

        BatchResult result = bankService.processBatch(batch);

        assertEquals(TASK_COUNT, result.getTotalRequested(), "Total requested tasks must match.");
        assertEquals(TASK_COUNT, result.getAccepted(), "All tasks must be accepted.");
        assertEquals(TASK_COUNT, result.getSucceeded(), "All concurrent deposit tasks must succeed.");
        assertEquals(0, result.getFailed(), "No concurrent deposit task should fail.");
        assertEquals(0, result.getRejected(), "No task should be rejected.");
        assertEquals(TASK_COUNT, result.getCompleted(), "Completed tasks must equal succeeded + failed.");

        long expectedBalance = 1_000 + (TASK_COUNT * DEPOSIT_AMT);
        assertEquals(expectedBalance, acc.getBalance(),
                "Final balance must reflect exact sum of all synchronized concurrent deposits.");
    }

    // -------------------------------------------------------------------------
    // Test 4 — Account not found
    // -------------------------------------------------------------------------
    @Test
    void testAccountNotFound() {
        assertThrows(
                AccountNotFoundException.class,
                () -> bankService.deposit("AC9999", 500),
                "Operating on a non-existent account number must throw AccountNotFoundException."
        );
    }

    // -------------------------------------------------------------------------
    // Test 5 — Invalid deposit amount
    // -------------------------------------------------------------------------
    @Test
    void testInvalidDepositAmount() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("David", 2_000, 500);

        assertThrows(
                InvalidAmountException.class,
                () -> bankService.deposit(acc.getAccountNumber(), 0),
                "Deposit of 0 must throw InvalidAmountException."
        );

        assertThrows(
                InvalidAmountException.class,
                () -> bankService.deposit(acc.getAccountNumber(), -50),
                "Negative deposit amount must throw InvalidAmountException."
        );
    }

    // -------------------------------------------------------------------------
    // Test 6 — Successful transfer
    // -------------------------------------------------------------------------
    @Test
    void testSuccessfulTransfer() throws Exception {
        SavingsAccount src  = bankService.openSavingsAccount("Eve", 6_000, 1_000);
        CurrentAccount dest = bankService.openCurrentAccount("Frank", 2_000, 500);

        bankService.transfer(src.getAccountNumber(), dest.getAccountNumber(), 2_000);

        assertEquals(4_000, src.getBalance(), "Source balance must be debited.");
        assertEquals(4_000, dest.getBalance(), "Destination balance must be credited.");
    }

    // -------------------------------------------------------------------------
    // Test 7 — Failed transfer leaves both balances unchanged
    // -------------------------------------------------------------------------
    @Test
    void testFailedTransferLeavesBalancesUnchanged() throws Exception {
        SavingsAccount src  = bankService.openSavingsAccount("Grace", 1_500, 1_000);
        SavingsAccount dest = bankService.openSavingsAccount("Heidi", 2_000, 500);

        long srcBefore  = src.getBalance();
        long destBefore = dest.getBalance();

        assertThrows(
                InsufficientFundsException.class,
                () -> bankService.transfer(src.getAccountNumber(), dest.getAccountNumber(), 1_000),
                "Transfer exceeding source capacity must throw InsufficientFundsException."
        );

        assertEquals(srcBefore, src.getBalance(), "Source balance must remain unchanged.");
        assertEquals(destBefore, dest.getBalance(), "Destination balance must remain unchanged.");
    }

    // -------------------------------------------------------------------------
    // Test 8 — Account persistence and reload
    // -------------------------------------------------------------------------
    @Test
    void testAccountPersistenceAndReload() throws Exception {
        SavingsAccount sa = bankService.openSavingsAccount("Ivan", 4_000, 800);
        CurrentAccount ca = bankService.openCurrentAccount("Judy", 7_000, 2_000);
        FixedDepositAccount fd = bankService.openFixedDepositAccount("Kevin", 15_000);

        Account[] toSave = { sa, ca, fd };
        Path tempFile = Files.createTempFile("minibank-test-persist", ".dat");

        try {
            StatePersister.save(toSave, tempFile);
            Account[] reloaded = StatePersister.load(tempFile);

            assertEquals(3, reloaded.length, "Must reload exact number of accounts.");
            assertEquals(sa.getAccountNumber(), reloaded[0].getAccountNumber());
            assertEquals(sa.getBalance(), reloaded[0].getBalance());
            assertTrue(reloaded[0] instanceof SavingsAccount, "Subclass type must be preserved.");
            assertEquals(800, ((SavingsAccount) reloaded[0]).getMinBalance(), "Subclass field must be preserved.");

            assertTrue(reloaded[1] instanceof CurrentAccount);
            assertEquals(2_000, ((CurrentAccount) reloaded[1]).getOverdraftLimit());

            assertTrue(reloaded[2] instanceof FixedDepositAccount);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    // -------------------------------------------------------------------------
    // Test 9 — Repository lookup after account creation
    // -------------------------------------------------------------------------
    @Test
    void testRepositoryLookupAfterAccountCreation() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("Leo", 3_500, 500);
        Account found = repository.findById(acc.getAccountNumber());

        assertNotNull(found, "Account must be immediately findable in repository.");
        assertSame(acc, found, "Repository must return the exact same account instance (single source of truth).");
    }

    // -------------------------------------------------------------------------
    // Test 10 — Monotonic account sequence recovery from logs and files
    // -------------------------------------------------------------------------
    @Test
    void testMonotonicAccountSequenceRecovery() throws Exception {
        Path tempDir = Files.createTempDirectory("minibank-seq-test");
        try {
            Path testLogsDir = tempDir.resolve("logs");
            Files.createDirectories(testLogsDir);
            Path testLog = testLogsDir.resolve("transactions-test.log");
            Files.writeString(testLog,
                    "2026-10-30T10:00:00Z|AC0088|DEPOSIT|5000\n" +
                    "2026-10-30T10:05:00Z|AC0050|WITHDRAW|1000\n");

            Path testAccFile = tempDir.resolve("accounts.dat");
            Path testSeqFile = tempDir.resolve("sequence.dat");

            long recovered = BankService.recoverAndSyncAccountSequence(testAccFile, testLogsDir, testSeqFile);
            assertTrue(recovered >= 88L, "Recovered counter must be at least 88 from logs.");

            SavingsAccount newAcc = bankService.openSavingsAccount("SeqTestUser", 2000, 500);
            long newNum = Long.parseLong(newAcc.getAccountNumber().substring(2));
            assertTrue(newNum > 88L, "Newly opened account number must strictly exceed 88.");
        } finally {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                      .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
            }
        }
    }

    // -------------------------------------------------------------------------
    // Test 11 — Statement displays only target account's transactions
    // -------------------------------------------------------------------------
    @Test
    void testAccountStatementContainsOnlyTargetAccountTransactions() throws Exception {
        SavingsAccount user1 = bankService.openSavingsAccount("UserOne", 3_000, 500);
        SavingsAccount user2 = bankService.openSavingsAccount("UserTwo", 4_000, 500);

        bankService.deposit(user1.getAccountNumber(), 1_500);
        bankService.deposit(user2.getAccountNumber(), 2_500);

        String statement1 = bankService.statement(user1.getAccountNumber());
        String statement2 = bankService.statement(user2.getAccountNumber());

        assertTrue(statement1.contains("1500"), "User 1 statement must contain its 1500 deposit.");
        assertFalse(statement1.contains("2500"), "User 1 statement must NOT contain User 2's 2500 deposit.");

        assertTrue(statement2.contains("2500"), "User 2 statement must contain its 2500 deposit.");
        assertFalse(statement2.contains("1500"), "User 2 statement must NOT contain User 1's 1500 deposit.");
    }

    // -------------------------------------------------------------------------
    // Test 12 — ISSUE 2: Audit log isolation and production file immutability
    // -------------------------------------------------------------------------
    @Test
    void testAuditLogIsolationAndProductionImmutability() throws Exception {
        Path prodAuditFile = Paths.get("data", "audit", "audit.log");
        String prodHashBefore = computeSha256(prodAuditFile);

        // Perform operations with @Audit annotations
        SavingsAccount sa = bankService.openSavingsAccount("AuditTestUser", 4_000, 500);
        bankService.deposit(sa.getAccountNumber(), 1_000);
        bankService.withdraw(sa.getAccountNumber(), 500);

        // Verify audit log was recorded in the temporary audit file
        assertTrue(Files.exists(tempAuditFile), "Temporary audit log must be created.");
        String tempAuditContent = Files.readString(tempAuditFile, StandardCharsets.UTF_8);
        assertTrue(tempAuditContent.contains("openSavingsAccount"), "Temporary audit log must contain method name.");
        assertTrue(tempAuditContent.contains(sa.getAccountNumber()), "Temporary audit log must contain account number.");

        // Verify production audit file was NOT touched
        String prodHashAfter = computeSha256(prodAuditFile);
        assertEquals(prodHashBefore, prodHashAfter,
                "Production audit log must remain strictly identical before and after test execution.");
    }

    // -------------------------------------------------------------------------
    // Test 13 — ISSUE 3: Successful batch processing
    // -------------------------------------------------------------------------
    @Test
    void testSuccessfulBatchProcessing() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("BatchSuccessUser", 5_000, 1_000);
        List<BatchTransaction> batch = new ArrayList<>();
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.DEPOSIT, 500));
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.WITHDRAW, 200));
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.DEPOSIT, 300));

        BatchResult result = bankService.processBatch(batch);

        assertEquals(3, result.getTotalRequested());
        assertEquals(3, result.getAccepted());
        assertEquals(3, result.getSucceeded());
        assertEquals(0, result.getFailed());
        assertEquals(0, result.getRejected());
        assertEquals(3, result.getCompleted());
        assertEquals(5_600, acc.getBalance());
        assertTrue(result.getErrors().isEmpty());
    }

    // -------------------------------------------------------------------------
    // Test 14 — ISSUE 3: Batch containing failed transactions
    // -------------------------------------------------------------------------
    @Test
    void testBatchContainingFailedTransactions() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("BatchFailUser", 2_000, 1_000);
        List<BatchTransaction> batch = new ArrayList<>();
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.DEPOSIT, 500)); // succeed -> 2500
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.WITHDRAW, 5_000)); // fail (insufficient)
        batch.add(new BatchTransaction("AC9999", TransactionType.DEPOSIT, 100)); // fail (not found)

        BatchResult result = bankService.processBatch(batch);

        assertEquals(3, result.getTotalRequested());
        assertEquals(3, result.getAccepted());
        assertEquals(1, result.getSucceeded());
        assertEquals(2, result.getFailed());
        assertEquals(0, result.getRejected());
        assertEquals(3, result.getCompleted());
        assertEquals(2, result.getErrors().size());
        assertEquals(2_500, acc.getBalance());
    }

    // -------------------------------------------------------------------------
    // Test 15 — ISSUE 3: Executor rejection when processor is stopped
    // -------------------------------------------------------------------------
    @Test
    void testBatchExecutorRejection() throws Exception {
        TransactionProcessor stoppedProc = new TransactionProcessor();
        stoppedProc.stop(); // Stop executor before submission

        BankService svcWithStoppedProc = new BankService(repository, stoppedProc, tempLogsDir, tempSeqFile, tempAuditFile);

        List<BatchTransaction> batch = new ArrayList<>();
        batch.add(new BatchTransaction("AC0001", TransactionType.DEPOSIT, 100));
        batch.add(new BatchTransaction("AC0001", TransactionType.DEPOSIT, 200));

        BatchResult result = svcWithStoppedProc.processBatch(batch);

        assertEquals(2, result.getTotalRequested());
        assertEquals(0, result.getAccepted(), "No tasks should be accepted by stopped processor.");
        assertEquals(2, result.getRejected(), "Both tasks must be counted as rejected.");
        assertEquals(0, result.getCompleted(), "Rejected tasks must not count as completed.");
        assertEquals(0, result.getSucceeded());
        assertEquals(0, result.getFailed());
        assertEquals(2, result.getErrors().size());
    }

    // -------------------------------------------------------------------------
    // Test 16 — ISSUE 3: Interrupted batch handling
    // -------------------------------------------------------------------------
    @Test
    void testBatchInterruptionHandling() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("InterruptUser", 3_000, 500);
        List<BatchTransaction> batch = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.DEPOSIT, 10));
        }

        AtomicBoolean threadInterrupted = new AtomicBoolean(false);
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt(); // Pre-interrupt thread
            bankService.processBatch(batch);
            if (Thread.currentThread().isInterrupted()) {
                threadInterrupted.set(true);
            }
        });

        testThread.start();
        testThread.join();

        assertTrue(threadInterrupted.get(), "Thread interrupt status must be preserved upon return.");
    }

    // -------------------------------------------------------------------------
    // Test 17 — ISSUE 3: Empty and null batch handling
    // -------------------------------------------------------------------------
    @Test
    void testEmptyAndNullBatch() {
        BatchResult r1 = bankService.processBatch(List.of());
        assertEquals(0, r1.getTotalRequested());
        assertEquals(0, r1.getAccepted());
        assertEquals(0, r1.getSucceeded());
        assertEquals(0, r1.getFailed());
        assertEquals(0, r1.getRejected());
        assertEquals(0, r1.getCompleted());

        BatchResult r2 = bankService.processBatch(null);
        assertEquals(0, r2.getTotalRequested());
        assertEquals(0, r2.getCompleted());
    }

    // -------------------------------------------------------------------------
    // Test 18 — ISSUE 3: Accurate final counts (succeeded + failed = completed)
    // -------------------------------------------------------------------------
    @Test
    void testBatchAccurateFinalCounts() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("CountUser", 4_000, 1_000);
        List<BatchTransaction> batch = new ArrayList<>();
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.DEPOSIT, 200));
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.WITHDRAW, 100));
        batch.add(new BatchTransaction(acc.getAccountNumber(), TransactionType.WITHDRAW, 8_000)); // fail

        BatchResult result = bankService.processBatch(batch);

        assertEquals(result.getAccepted(), result.getCompleted(), "accepted == completed");
        assertEquals(result.getCompleted(), result.getSucceeded() + result.getFailed(), "completed == succeeded + failed");
        assertEquals(result.getTotalRequested(), result.getAccepted() + result.getRejected(), "total == accepted + rejected");
    }

    // -------------------------------------------------------------------------
    // Test 19 — ISSUE 4: Repository and processor registry reference identity
    // -------------------------------------------------------------------------
    @Test
    void testRepositoryAndRegistryReferenceIdentity() throws Exception {
        SavingsAccount created = bankService.openSavingsAccount("IdentityUser", 6_000, 1_000);

        Account inRepo = repository.findById(created.getAccountNumber());
        Account inProc = processor.getAccount(created.getAccountNumber());

        assertNotNull(inRepo);
        assertNotNull(inProc);
        assertSame(created, inRepo, "Repository must hold exact object instance.");
        assertSame(inRepo, inProc, "Processor registry must hold the exact same object reference as repository.");
    }

    // -------------------------------------------------------------------------
    // Test 20 — ISSUE 4: Duplicate account registration rejected
    // -------------------------------------------------------------------------
    @Test
    void testDuplicateAccountRegistrationRejected() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("DupUser", 5_000, 500);

        SavingsAccount dup = new SavingsAccount("Duplicate", 1_000, 100);
        // Attempting to register the same account ID must throw IllegalStateException
        assertThrows(IllegalStateException.class, () -> processor.registerAccount(acc),
                "Registering an already-registered account must throw IllegalStateException.");
    }

    // -------------------------------------------------------------------------
    // Test 21 — ISSUE 4: Restored accounts available to batch processing
    // -------------------------------------------------------------------------
    @Test
    void testRestoredAccountsAvailableToBatchProcessing() throws Exception {
        SavingsAccount sa = new SavingsAccount("RestoredUser", 3_000, 500);
        repository.save(sa);
        processor.registerAccount(sa);

        List<BatchTransaction> batch = List.of(
                new BatchTransaction(sa.getAccountNumber(), TransactionType.DEPOSIT, 500)
        );

        BatchResult result = bankService.processBatch(batch);
        assertEquals(1, result.getSucceeded());
        assertEquals(3_500, sa.getBalance());
        assertSame(repository.findById(sa.getAccountNumber()), processor.getAccount(sa.getAccountNumber()));
    }

    // -------------------------------------------------------------------------
    // Test 22 — ISSUE 6: Chronological sorting across multiple daily log files
    // -------------------------------------------------------------------------
    @Test
    void testAccountStatementChronologicalSortingMultipleFiles() throws Exception {
        SavingsAccount acc = bankService.openSavingsAccount("ChronoUser", 10_000, 1_000);
        String accNum = acc.getAccountNumber();

        // Write historical log for 2026-10-25 with early timestamp
        Path logDay1 = tempLogsDir.resolve("transactions-2026-10-25.log");
        Files.writeString(logDay1,
                "2026-10-25T08:00:00Z|" + accNum + "|DEPOSIT|111\n" +
                "2026-10-25T14:00:00Z|" + accNum + "|WITHDRAW|50\n");

        // Write historical log for 2026-10-26 with later timestamp
        Path logDay2 = tempLogsDir.resolve("transactions-2026-10-26.log");
        Files.writeString(logDay2,
                "2026-10-26T09:30:00Z|" + accNum + "|DEPOSIT|222\n");

        String statement = bankService.statement(accNum);

        int pos111 = statement.indexOf("111");
        int pos50  = statement.indexOf("50");
        int pos222 = statement.indexOf("222");

        assertTrue(pos111 != -1 && pos50 != -1 && pos222 != -1, "All transactions must be present in statement.");
        assertTrue(pos111 < pos50, "08:00:00 deposit must appear before 14:00:00 withdrawal.");
        assertTrue(pos50 < pos222, "Day 1 14:00:00 withdrawal must appear before Day 2 09:30:00 deposit.");
    }

    // -------------------------------------------------------------------------
    // Test 23 — ISSUE 7: Honest transaction-log write error propagation
    // -------------------------------------------------------------------------
    @Test
    void testTransactionLogWriteErrorPropagatedHonestly() throws Exception {
        // Create an invalid log path (a regular file where a directory is expected)
        Path invalidLogsDir = tempBaseDir.resolve("invalid_logs_file");
        Files.writeString(invalidLogsDir, "I am a file, not a directory");

        BankService svcWithBadLogs = new BankService(repository, processor, invalidLogsDir, tempSeqFile, tempAuditFile);
        SavingsAccount acc = new SavingsAccount("LogFailUser", 5_000, 1_000);
        repository.save(acc);
        processor.registerAccount(acc);

        TransactionLoggingException ex = assertThrows(
                TransactionLoggingException.class,
                () -> svcWithBadLogs.deposit(acc.getAccountNumber(), 1_000),
                "Deposit with failing transaction log must throw TransactionLoggingException."
        );

        // Verify account balance was updated to 6000 and NOT rolled back
        assertEquals(6_000, acc.getBalance(), "Financial update must succeed despite logging failure.");
        assertEquals(acc.getAccountNumber(), ex.getAccountNumber());
        assertEquals(6_000, ex.getCurrentBalance());
    }

    // -------------------------------------------------------------------------
    // Test 24 — Quality Fix B: Comprehensive sequence recovery across all sources
    // -------------------------------------------------------------------------
    @Test
    void testComprehensiveSequenceRecoveryAcrossAllSources() throws Exception {
        Path tempDir = Files.createTempDirectory("minibank-comp-seq");
        try {
            Path testLogsDir = tempDir.resolve("logs");
            Files.createDirectories(testLogsDir);
            Files.writeString(testLogsDir.resolve("tx-2026.log"),
                    "2026-10-30T10:00:00Z|AC0095|DEPOSIT|1000\n");

            Path testAccFile = tempDir.resolve("accounts.dat");
            Path testSeqFile = tempDir.resolve("sequence.dat");
            Files.writeString(testSeqFile, "80\n");

            long maxRecovered = BankService.recoverAndSyncAccountSequence(testAccFile, testLogsDir, testSeqFile);
            assertTrue(maxRecovered >= 95L, "Must recover at least 95 from log file.");

            BankService testSvc = new BankService(new Repository<>(), new TransactionProcessor(),
                    testLogsDir, testSeqFile, tempDir.resolve("audit.log"));
            SavingsAccount newlyOpened = testSvc.openSavingsAccount("CompSeqUser", 1000, 100);
            long allocated = Long.parseLong(newlyOpened.getAccountNumber().substring(2));
            assertTrue(allocated > 95L, "Newly opened account must receive ID strictly > 95.");
            testSvc.getProcessor().stop();
        } finally {
            try (var stream = Files.walk(tempDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                      .forEach(p -> { try { Files.deleteIfExists(p); } catch (Exception ignored) {} });
            }
        }
    }
}
