package service;

import exception.BankException;
import model.Account;
import model.TransactionType;

/**
 * A Runnable transaction task that performs one deposit or withdrawal
 * on a shared account when executed by the thread pool.
 */
public class AccountTransactionTask implements Runnable {

    private final Account         account;
    private final TransactionType type;
    private final long            amount;

    public AccountTransactionTask(Account account, TransactionType type, long amount) {
        this.account = account;
        this.type    = type;
        this.amount  = amount;
    }

    @Override
    public void run() {
        try {
            switch (type) {
                case DEPOSIT  -> account.deposit(amount);
                case WITHDRAW -> account.withdraw(amount);
                default       -> System.err.println("Unsupported transaction type: " + type);
            }
        } catch (BankException e) {
            System.err.printf("[%s] Task failed (%s ₹%d): %s%n",
                    Thread.currentThread().getName(), type, amount, e.getMessage());
        }
    }
}
