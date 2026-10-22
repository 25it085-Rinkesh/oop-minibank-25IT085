package exception;

/**
 * Thrown when a zero or negative amount is supplied to a transaction.
 */
public class InvalidAmountException extends BankException {

    public InvalidAmountException(String message) {
        super(message);
    }
}
