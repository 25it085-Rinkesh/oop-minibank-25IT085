package exception;

/**
 * Thrown when a withdrawal or transfer is rejected because the account
 * does not have enough available funds.
 *
 * <p>The {@code shortfall} field captures how much more money would be
 * required to complete the transaction. It is always non-negative.
 */
public class InsufficientFundsException extends BankException {

    private final long shortfall;

    /**
     * @param message   human-readable description of the failure
     * @param shortfall how much additional balance would be needed;
     *                  clamped to {@code 0} if a negative value is supplied
     */
    public InsufficientFundsException(String message, long shortfall) {
        super(message);
        this.shortfall = Math.max(0L, shortfall);
    }

    /** Returns the non-negative amount by which the available balance was insufficient. */
    public long getShortfall() {
        return shortfall;
    }
}
