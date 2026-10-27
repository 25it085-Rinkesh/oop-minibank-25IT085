package service;

import exception.BankException;
import model.Account;

/**
 * Runnable worker that deposits a fixed amount into a shared account a
 * specified number of times. Used to demonstrate concurrent-deposit behaviour
 * (both the race condition and the corrected, synchronized version).
 */
public class AccountWorker implements Runnable {

    private final Account account;
    private final int     times;
    private final long    amount;

    public AccountWorker(Account account, int times, long amount) {
        this.account = account;
        this.times   = times;
        this.amount  = amount;
    }

    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + " started.");
        for (int i = 0; i < times; i++) {
            try {
                account.deposit(amount);
            } catch (BankException e) {
                // An overflow or invalid-amount during the concurrent test is a
                // real failure that must not be swallowed silently.
                System.err.println(Thread.currentThread().getName()
                        + " deposit failed unexpectedly: " + e.getMessage());
            }
        }
        System.out.println(Thread.currentThread().getName() + " finished.");
    }
}
