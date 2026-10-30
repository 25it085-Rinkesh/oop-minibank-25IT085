package test.service;

import exception.AccountNotFoundException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JUnit 5 test suite for BankService and core MiniBank operations.
 */
public class BankServiceTest {

    private Repository<Account>  repository;
    private TransactionProcessor processor;
    private BankService          bankService;
    private Path                 tempLogsDir;

    @BeforeEach
    void setUp() throws IOException {
        repository  = new Repository<>();
        processor   = new TransactionProcessor();
        tempLogsDir = Files.createTempDirectory("minibank-test-logs");
        bankService = new BankService(repository, processor, tempLogsDir, tempLogsDir.resolve("sequence.dat"));
    }

    @AfterEach
    void tearDown() throws IOException {
        processor.stop();
        // Clean up temporary log files
        if (Files.exists(tempLogsDir)) {
            try (var stream = Files.walk(tempLogsDir)) {
                stream.sorted((a, b) -> b.compareTo(a))
                      .forEach(p -> {
                          try { Files.deleteIfExists(p); } catch (Exception ignored) {}
                      });
            }
        }
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

        // Limit is balance - minBalance = 3000 - 1000 = 2000. Attempt 2500.
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

        assertEquals(TASK_COUNT, result.getTotal(), "Total submitted tasks must match.");
        assertEquals(TASK_COUNT, result.getSucceeded(), "All concurrent deposit tasks must succeed.");
        assertEquals(0, result.getFailed(), "No concurrent deposit task should fail.");

        long expectedBalance = 1_000 + (TASK_COUNT * DEPOSIT_AMT);
        assertEquals(expectedBalance, acc.getBalance(),
                "Final balance must reflect exact sum of all synchronized concurrent deposits.");
    }

    // -------------------------------------------------------------------------
    // Additional Test 4 — Account not found
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
    // Additional Test 5 — Invalid deposit amount
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
    // Additional Test 6 — Successful transfer
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
    // Additional Test 7 — Failed transfer leaves both balances unchanged
    // -------------------------------------------------------------------------
    @Test
    void testFailedTransferLeavesBalancesUnchanged() throws Exception {
        SavingsAccount src  = bankService.openSavingsAccount("Grace", 1_500, 1_000);
        SavingsAccount dest = bankService.openSavingsAccount("Heidi", 2_000, 500);

        long srcBefore  = src.getBalance();
        long destBefore = dest.getBalance();

        // Attempting to transfer 1000 leaves 500 < 1000 minBalance
        assertThrows(
                InsufficientFundsException.class,
                () -> bankService.transfer(src.getAccountNumber(), dest.getAccountNumber(), 1_000),
                "Transfer exceeding source capacity must throw InsufficientFundsException."
        );

        assertEquals(srcBefore, src.getBalance(), "Source balance must remain unchanged.");
        assertEquals(destBefore, dest.getBalance(), "Destination balance must remain unchanged.");
    }

    // -------------------------------------------------------------------------
    // Additional Test 8 — Account persistence and reload
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
    // Additional Test 9 — Repository lookup after account creation
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

        // user1 statement must show user1 deposit (1500) and opening (3000), but NOT 2500
        assertTrue(statement1.contains("1500"), "User 1 statement must contain its 1500 deposit.");
        assertFalse(statement1.contains("2500"), "User 1 statement must NOT contain User 2's 2500 deposit.");

        // user2 statement must show user2 deposit (2500) and opening (4000), but NOT 1500
        assertTrue(statement2.contains("2500"), "User 2 statement must contain its 2500 deposit.");
        assertFalse(statement2.contains("1500"), "User 2 statement must NOT contain User 1's 1500 deposit.");
    }
}
