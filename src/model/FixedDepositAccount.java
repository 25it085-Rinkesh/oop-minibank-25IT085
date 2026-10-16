package model;

public class FixedDepositAccount extends Account {

    public FixedDepositAccount(String ownerName, long openingBalance) {
        super(ownerName, openingBalance);
    }

    @Override
    public double interestRate() {
        return 7.0;
    }

    @Override
    public boolean canWithdraw(long amount) {
        // Fixed deposits are locked; no withdrawals permitted.
        return false;
    }
}
