public class SavingsAccount extends Account {

    private long minBalance;

    public SavingsAccount(String ownerName, long openingBalance, long minBalance) {
        super(ownerName, openingBalance);
        this.minBalance = (minBalance >= 0) ? minBalance : 0;
    }

    public long getMinBalance() {
        return minBalance;
    }

    @Override
    public double interestRate() {
        return 4.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        if (amount <= 0) {
            return false;
        }
        // Ensure balance - amount >= minBalance without underflow.
        // Equivalent to: balance >= amount + minBalance, checked safely.
        long balance = getBalance();
        return balance - amount >= minBalance;
    }
}
