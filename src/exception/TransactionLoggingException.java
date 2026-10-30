package exception;

/**
 * Checked exception thrown when an essential financial balance modification has succeeded,
 * but recording the transaction to the append-only log failed due to an I/O error.
 *
 * <p>This exception carries the affected account number and the updated balance so callers
 * can honestly distinguish a logging failure from a failed banking operation without
 * attempting duplicate transactions or rolling back safely committed updates.
 */
public class TransactionLoggingException extends BankException {

    private final String accountNumber;
    private final long   currentBalance;

    public TransactionLoggingException(String message, String accountNumber, long currentBalance, Throwable cause) {
        super(message, cause);
        this.accountNumber  = accountNumber;
        this.currentBalance = currentBalance;
    }

    public TransactionLoggingException(String message, Throwable cause) {
        this(message, null, -1, cause);
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public long getCurrentBalance() {
        return currentBalance;
    }
}
