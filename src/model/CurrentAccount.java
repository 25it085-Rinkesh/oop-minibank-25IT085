package model;

public class CurrentAccount extends Account {

    private long overdraftLimit;

    public CurrentAccount(String ownerName, long openingBalance, long overdraftLimit) {
        super(ownerName, openingBalance);
        if (overdraftLimit < 0) {
            throw new IllegalArgumentException("overdraftLimit cannot be negative: " + overdraftLimit);
        }
        this.overdraftLimit = overdraftLimit;
    }

    public long getOverdraftLimit() {
        return overdraftLimit;
    }

    @Override
    public double interestRate() {
        return 0.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        if (amount <= 0) {
            return false;
        }
        // Rule: balance - amount >= -overdraftLimit
        // Rearranged to avoid overflow: balance >= amount - overdraftLimit
        // Both amount and overdraftLimit are nonnegative, so their difference
        // is in [-Long.MAX_VALUE, Long.MAX_VALUE] — always representable.
        // The old form  balance + overdraftLimit >= amount  could overflow
        // when balance is very large and overdraftLimit is also large.
        long balance = getBalance();
        return balance >= amount - overdraftLimit;
    }
}
