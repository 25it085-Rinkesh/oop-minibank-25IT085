import java.util.Objects;

public abstract class Account {
    private static long accountCounter = 0;

    private final String accountNumber;
    private String ownerName;
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

    // Each subclass declares its annual interest rate.
    public abstract double interestRate();

    // Each subclass decides whether a withdrawal of this amount is permitted.
    public abstract boolean canWithdraw(long amount);

    public void deposit(long amount) {
        if (amount > 0 && Long.MAX_VALUE - balance >= amount) {
            balance += amount;
        }
    }

    // Delegates withdrawal eligibility to the subclass via canWithdraw().
    // This is runtime polymorphism: no subclass names appear here.
    public boolean withdraw(long amount) {
        if (amount <= 0) {
            return false;
        }
        if (!canWithdraw(amount)) {
            return false;
        }
        // Guard against arithmetic underflow before subtracting.
        if (balance - amount < Long.MIN_VALUE) {
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