package service;

import model.Account;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Objects;

/**
 * In-memory account store backed by a {@link HashMap}.
 *
 * <p>AccountStore is intentionally single-threaded; use the synchronised
 * methods on {@link Account} for concurrent balance changes.
 */
public class AccountStore {

    private final HashMap<String, Account>  accounts           = new HashMap<>();
    private final ArrayList<String>         transactionHistory = new ArrayList<>();

    // -----------------------------------------------------------------------
    // Account CRUD
    // -----------------------------------------------------------------------

    /**
     * Adds {@code account} to the store.
     *
     * @throws NullPointerException     if account is null
     * @throws IllegalArgumentException if an account with the same number already exists
     */
    public void add(Account account) {
        Objects.requireNonNull(account, "account must not be null");
        String key = account.getAccountNumber();
        if (accounts.containsKey(key)) {
            throw new IllegalArgumentException(
                    "Account already exists: " + key);
        }
        accounts.put(key, account);
    }

    /**
     * Returns the account with the given number, or {@code null} if not found.
     *
     * @throws IllegalArgumentException if accountNumber is null or blank
     */
    public Account get(String accountNumber) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be null or blank");
        }
        return accounts.get(accountNumber);
    }

    /**
     * Returns a snapshot of all stored accounts.
     */
    public ArrayList<Account> getAll() {
        return new ArrayList<>(accounts.values());
    }

    // -----------------------------------------------------------------------
    // Transaction history
    // -----------------------------------------------------------------------

    /**
     * Records a transaction description.
     *
     * @throws IllegalArgumentException if transaction is null or blank
     */
    public void recordTransaction(String transaction) {
        if (transaction == null || transaction.isBlank()) {
            throw new IllegalArgumentException("transaction entry must not be null or blank");
        }
        transactionHistory.add(transaction);
    }

    /**
     * Returns a defensive copy of the transaction history in insertion order.
     */
    public ArrayList<String> getTransactionHistory() {
        return new ArrayList<>(transactionHistory);
    }

    // -----------------------------------------------------------------------
    // Sorting
    // -----------------------------------------------------------------------

    /**
     * Returns a new list of all accounts sorted by ascending balance,
     * with account number as a deterministic tie-breaker.
     */
    public ArrayList<Account> sortedByBalance() {
        ArrayList<Account> sorted = new ArrayList<>(accounts.values());
        sorted.sort(Comparator.comparingLong(Account::getBalance)
                              .thenComparing(Account::getAccountNumber));
        return sorted;
    }
}
