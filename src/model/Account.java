package model;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Objects;
import model.annotation.Id;
import model.annotation.MaxLength;
import model.annotation.Positive;

public abstract class Account implements Transactable, InterestBearing, Serializable, Comparable<Account> {

    private static final long serialVersionUID = 1L;

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

    private static synchronized String generateAccountNumber() {
        accountCounter++;
        return String.format("AC%04d", accountCounter);
    }

    /**
     * After deserialization, bump the static counter to at least the numeric
     * portion of the restored account number so newly constructed accounts do
     * not collide with already-serialized ones.
     */
    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // accountNumber has the form "AC0042"; parse the trailing digits.
        if (accountNumber != null && accountNumber.startsWith("AC")) {
            try {
                long restored = Long.parseLong(accountNumber.substring(2));
                synchronized (Account.class) {
                    if (restored > accountCounter) {
                        accountCounter = restored;
                    }
                }
            } catch (NumberFormatException ignored) {
                // Non-standard account number — leave counter unchanged.
            }
        }
    }

    @Override
    public abstract double interestRate();

    public abstract boolean canWithdraw(long amount);

    /**
     * Returns the maximum amount that may be withdrawn from this account right
     * now under its specific rules (minimum-balance floor, overdraft limit, or
     * locked status).  Always non-negative.
     */
    public abstract long maxWithdrawable();

    @Override
    public synchronized void deposit(long amount) throws InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "Deposit amount must be positive, but was: " + amount);
        }
        // Safe overflow check: rearranged to avoid subtraction from Long.MAX_VALUE
        // when balance is negative. Since amount > 0, Long.MAX_VALUE - amount is
        // always representable, so comparing balance <= Long.MAX_VALUE - amount is safe.
        if (balance > Long.MAX_VALUE - amount) {
            throw new InvalidAmountException(
                    "Deposit of " + amount + " would overflow the account balance.");
        }
        balance += amount;
    }

    @Override
    public synchronized boolean withdraw(long amount) throws InsufficientFundsException, InvalidAmountException {
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "Withdrawal amount must be positive, but was: " + amount);
        }
        // Safe underflow precondition checked BEFORE canWithdraw() and before
        // any subtraction. Rearranged: balance - amount >= Long.MIN_VALUE
        // <=> balance >= Long.MIN_VALUE + amount (safe since amount > 0).
        if (balance < Long.MIN_VALUE + amount) {
            throw new InsufficientFundsException(
                    "Withdrawal of " + amount + " would underflow the account balance.", 0L);
        }
        if (!canWithdraw(amount)) {
            // Shortfall = how much the requested amount exceeds the maximum
            // currently withdrawable under this account's specific rules.
            // maxWithdrawable() is always >= 0 and amount > 0, so the
            // subtraction is safe from overflow (result fits in a long).
            long shortfall = amount - maxWithdrawable();
            throw new InsufficientFundsException(
                    "Withdrawal of " + amount + " is not permitted on this account "
                            + "(balance: " + balance + ").",
                    shortfall);
        }
        balance -= amount;
        return true;
    }

    /**
     * Transfers {@code amount} from this account to {@code to}.
     *
     * <p>Locks are always acquired in account-number order (lexicographically
     * smaller account number first) to prevent deadlock when two threads
     * transfer between the same pair of accounts simultaneously.
     *
     * <p>If the source is debited but the destination credit fails, the source
     * is credited back (rollback). Any rollback failure is attached as a
     * suppressed exception before rethrowing.
     *
     * @throws AccountNotFoundException   if {@code to} is null or this == to
     * @throws InvalidAmountException     if {@code amount <= 0}
     * @throws InsufficientFundsException if this account cannot cover the withdrawal
     * @throws BankException              for any other bank-side failure
     */
    public void transfer(Account to, long amount) throws BankException {
        if (to == null) {
            throw new AccountNotFoundException(
                    "Transfer failed: destination account is null.");
        }
        if (this == to) {
            throw new BankException(
                    "Transfer failed: source and destination are the same account ("
                            + accountNumber + ").");
        }
        if (amount <= 0) {
            throw new InvalidAmountException(
                    "Transfer amount must be positive, but was: " + amount);
        }

        // Consistent lock ordering: always lock the account with the
        // lexicographically smaller account number first. This eliminates
        // the circular-wait condition that causes deadlock.
        Account first;
        Account second;
        if (this.accountNumber.compareTo(to.accountNumber) <= 0) {
            first  = this;
            second = to;
        } else {
            first  = to;
            second = this;
        }

        synchronized (first) {
            synchronized (second) {
                // Pre-check: destination must be able to accept the funds.
                // Safe overflow check: amount > 0, so Long.MAX_VALUE - amount is representable.
                if (to.balance > Long.MAX_VALUE - amount) {
                    throw new InvalidAmountException(
                            "Transfer of " + amount + " would overflow the destination account balance.");
                }

                boolean debited = false;

                try {
                    withdraw(amount);
                    debited = true;
                    to.deposit(amount);
                } catch (BankException e) {
                    if (debited) {
                        // Rollback: credit the amount back to the source.
                        try {
                            deposit(amount);
                        } catch (InvalidAmountException rollbackEx) {
                            // Rollback itself failed — attach as suppressed so the
                            // original failure is still the primary exception.
                            e.addSuppressed(rollbackEx);
                        }
                    }
                    throw e;
                } finally {
                    // In a production system this block would flush a transaction log
                    // or release a distributed lock held for the duration of the transfer.
                }
            }
        }
    }

    /**
     * Natural ordering: lexicographical by account number.
     * Balance sorting belongs to the Comparator in AccountStore.
     */
    @Override
    public int compareTo(Account other) {
        Objects.requireNonNull(other, "cannot compare to a null Account");
        return this.accountNumber.compareTo(other.accountNumber);
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
