public class CurrentAccount extends Account {

    private long overdraftLimit;

    public CurrentAccount(String ownerName, long openingBalance, long overdraftLimit) {
        super(ownerName, openingBalance);
        this.overdraftLimit = (overdraftLimit >= 0) ? overdraftLimit : 0;
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
        // Balance may go negative, but must stay >= -overdraftLimit.
        // Safe check: balance - amount >= -overdraftLimit
        // Rearranged: balance + overdraftLimit >= amount
        long balance = getBalance();
        return balance + overdraftLimit >= amount;
    }
}
