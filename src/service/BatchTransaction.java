package service;

import model.TransactionType;

/**
 * Encapsulates an individual transaction instruction within a batch.
 */
public class BatchTransaction {

    private final String          accountNumber;
    private final TransactionType type;
    private final long            amount;

    public BatchTransaction(String accountNumber, TransactionType type, long amount) {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("accountNumber must not be null or blank");
        }
        if (type == null) {
            throw new IllegalArgumentException("transaction type must not be null");
        }
        this.accountNumber = accountNumber;
        this.type          = type;
        this.amount        = amount;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public TransactionType getType() {
        return type;
    }

    public long getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        return "BatchTransaction{" +
                "accountNumber='" + accountNumber + '\'' +
                ", type=" + type +
                ", amount=" + amount +
                '}';
    }
}
