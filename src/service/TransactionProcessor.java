package service;

import model.Account;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages a fixed pool of four threads for processing bank transaction tasks.
 *
 * <p>Also maintains a thread-safe {@link ConcurrentHashMap} registry of shared
 * {@link Account} objects so that worker tasks can resolve accounts by account
 * number without requiring external synchronization on the map itself.
 */
public class TransactionProcessor {

    private static final int  POOL_SIZE            = 4;
    private static final long SHUTDOWN_TIMEOUT_SEC = 10;

    private final ExecutorService executor =
            Executors.newFixedThreadPool(POOL_SIZE);

    private final ConcurrentHashMap<String, Account> sharedAccounts =
            new ConcurrentHashMap<>();

    // -----------------------------------------------------------------------
    // ExecutorService — task submission and lifecycle
    // -----------------------------------------------------------------------

    /**
     * Submits a task for execution on the thread pool.
     *
     * @param task the runnable task to execute; must not be null
     * @throws NullPointerException  if task is null
     * @throws IllegalStateException if the executor has already been shut down
     */
    public void submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task must not be null");
        }
        if (executor.isShutdown()) {
            throw new IllegalStateException("TransactionProcessor has been shut down");
        }
        executor.execute(task);
    }

    /**
     * Initiates an orderly shutdown, waiting up to {@value SHUTDOWN_TIMEOUT_SEC} seconds
     * for submitted tasks to complete. If tasks do not finish in time, forces shutdown
     * and waits a further bounded period.
     */
    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(SHUTDOWN_TIMEOUT_SEC, TimeUnit.SECONDS)) {
                    System.err.println("TransactionProcessor: executor did not terminate cleanly.");
                }
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // -----------------------------------------------------------------------
    // ConcurrentHashMap account registry
    // -----------------------------------------------------------------------

    /**
     * Registers an account in the shared registry using its account number as the key.
     *
     * <p>Uses {@code putIfAbsent()} so an existing registration is never silently replaced.
     *
     * @param account non-null account to register
     * @throws NullPointerException     if account is null
     * @throws IllegalStateException    if an account with the same number is already registered
     */
    public void registerAccount(Account account) {
        Objects.requireNonNull(account, "account must not be null");
        Account existing = sharedAccounts.putIfAbsent(account.getAccountNumber(), account);
        if (existing != null) {
            throw new IllegalStateException(
                    "Account already registered: " + account.getAccountNumber());
        }
    }

    /**
     * Returns the registered account for the given account number, or {@code null} if absent.
     *
     * @throws IllegalArgumentException if accountNumber is null or blank
     */
    public Account getAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be null or blank");
        }
        return sharedAccounts.get(accountNumber);
    }

    /**
     * Returns a snapshot of all registered accounts.
     */
    public ArrayList<Account> getAllAccounts() {
        return new ArrayList<>(sharedAccounts.values());
    }
}
