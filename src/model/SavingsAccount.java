package model;

public class SavingsAccount extends Account {

    private long minBalance;

    public SavingsAccount(String ownerName, long openingBalance, long minBalance) {
        super(ownerName, openingBalance);
        if (minBalance < 0) {
            throw new IllegalArgumentException("minBalance cannot be negative: " + minBalance);
        }
        this.minBalance = minBalance;
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
        // Rule: balance - amount >= minBalance
        // The expression balance - amount can underflow when balance is near
        // Long.MIN_VALUE. Safe equivalent (minBalance >= 0 is guaranteed by
        // the constructor clamp):
        //   Step 1: if balance < minBalance the check can never pass.
        //   Step 2: balance - minBalance >= amount, where balance - minBalance
        //           is nonnegative (step 1 ensures balance >= minBalance), so
        //           no underflow is possible.
        long balance = getBalance();
        if (balance < minBalance) {
            return false;
        }
        return balance - minBalance >= amount;
    }
}
