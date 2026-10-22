package exception;

/**
 * Thrown when a transfer destination account does not exist or is null.
 */
public class AccountNotFoundException extends BankException {

    public AccountNotFoundException(String message) {
        super(message);
    }
}
