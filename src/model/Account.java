package model;

import java.util.Objects;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

public abstract class Account implements Transactable, InterestBearing {
    private static long accountCounter = 0;

    @Id
    private final String accountNumber;
    @MaxLength(50)
    private String ownerName;
    @Positive
    private long balance;
    private boolean active;

    public Account(String ownerName, long openingBalance) {
        this.accountNumber = generateAccountNumber();
        this.ownerName = ownerName;
        this.balance = openingBalance;
        this.active = true;
    }

    public Account(String ownerName) {
        this(ownerName, 0);
    }

    private static String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    @Override
    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    @Override
    public void deposit(long amount) {
        // Safe overflow check: rearranged to avoid subtraction from Long.MAX_VALUE
        // when balance is negative. Since amount > 0, Long.MAX_VALUE - amount is
        // always representable, so comparing balance <= Long.MAX_VALUE - amount is safe.
        if (amount > 0 && balance <= Long.MAX_VALUE - amount) {
            balance += amount;
        }
    }

    @Override
    public boolean withdraw(long amount) {
        if (amount <= 0) {
            return false;
        }
        // Safe underflow precondition checked BEFORE canWithdraw() and before
        // any subtraction. The expression `balance - amount < Long.MIN_VALUE`
        // is always false in Java (long cannot be less than Long.MIN_VALUE),
        // so that check gave no protection. Instead, rearrange:
        //   balance - amount >= Long.MIN_VALUE
        //   <=> balance >= Long.MIN_VALUE + amount
        // Because amount > 0, Long.MIN_VALUE + amount is representable.
        if (balance < Long.MIN_VALUE + amount) {
            return false;
        }
        if (!canWithdraw(amount)) {
            return false;
        }
        balance -= amount;
        return true;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getOwnerName() {
        return ownerName;
    }

    @Override
    public long getBalance() {
        return balance;
    }

    public boolean isActive() {
        return active;
    }

    @Override
    public String toString() {
        return "Account{accountNumber='" + accountNumber + "', ownerName='" + ownerName + "', balance=" + balance + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Account account = (Account) o;
        return Objects.equals(accountNumber, account.accountNumber);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountNumber);
    }
}
