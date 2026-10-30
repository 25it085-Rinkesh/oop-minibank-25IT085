package model;

import java.io.Serializable;

public class FixedDepositAccount extends Account implements Serializable {

    private static final long serialVersionUID = 1L;

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

    @Override
    public long maxWithdrawable() {
        // Locked account — nothing can ever be withdrawn.
        return 0L;
    }
}
