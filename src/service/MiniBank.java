package service;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InsufficientFundsException;
import exception.InvalidAmountException;
import model.Account;
import model.BankInfo;
import model.Command;
import model.CurrentAccount;
import model.Customer;
import model.FixedDepositAccount;
import model.MenuOption;
import model.Premium;
import model.SavingsAccount;
import util.CommandParser;
import util.StatementFormatter;
import util.Validator;
import util.AnnotationValidator;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import model.TransactionType;

import static util.Validator.isValidMobile;

public class MiniBank {

    private static final String SEP = "----------------------------------------";

    public static void main(String[] args) {

        // Unexpected exceptions from demonstrations surface here as a friendly message.
        try {
            runDemonstrations();
        } catch (Exception e) {
            System.out.println("Demonstration encountered an unexpected error: " + e.getMessage());
        }

        BankInfo bank = new BankInfo("MiniBank", "Navsari");

        System.out.println("\n==================================");
        System.out.println("       WELCOME TO MINIBANK");
        System.out.println("==================================");
        System.out.println("Bank Name : " + bank.name());
        System.out.println("Branch    : " + bank.branch());

        Scanner sc = new Scanner(System.in);
        int choice = -1;

        try {
            do {
                System.out.println("\n---------- Menu ----------");
                System.out.println("1. Open Account");
                System.out.println("2. Deposit");
                System.out.println("3. Withdraw");
                System.out.println("4. Transfer");
                System.out.println("5. Exit");
                System.out.print("Enter your choice: ");

                if (!sc.hasNextInt()) {
                    if (!sc.hasNext()) {
                        break;
                    }
                    sc.next();
                    System.out.println("Invalid input! Please enter a number from 1 to 5.");
                    continue;
                }

                choice = sc.nextInt();

                MenuOption option = switch (choice) {
                    case 1 -> MenuOption.OPEN_ACCOUNT;
                    case 2 -> MenuOption.DEPOSIT;
                    case 3 -> MenuOption.WITHDRAW;
                    case 4 -> MenuOption.TRANSFER;
                    case 5 -> MenuOption.EXIT;
                    default -> null;
                };

                if (option == null) {
                    System.out.println("Invalid choice! Please try again.");
                    continue;
                }

                String message = switch (option) {
                    case OPEN_ACCOUNT -> "Open Account - To be implemented in a later lab.";
                    case DEPOSIT      -> "Deposit - To be implemented in a later lab.";
                    case WITHDRAW     -> "Withdraw - To be implemented in a later lab.";
                    case TRANSFER     -> "Transfer - To be implemented in a later lab.";
                    case EXIT         -> "Thank you for using MiniBank. Goodbye!";
                };

                System.out.println(message);

            } while (choice != 5);
        } finally {
            sc.close();
        }
    }

    private static void runDemonstrations() {
        SavingsAccount savings      = new SavingsAccount("Rinkesh", 5000, 1000);
        CurrentAccount current      = new CurrentAccount("Priya", 2000, 1000);
        FixedDepositAccount fixedDeposit = new FixedDepositAccount("Arjun", 10000);
        Account[] accounts = { savings, current, fixedDeposit };

        demoPolymorphism(accounts);
        demoPatternMatching(accounts, savings);
        demoWithdrawalRules(savings, current, fixedDeposit);
        demoInsufficientFunds();
        demoEquality(accounts, savings, current);
        demoCustomer();
        demoValidator();
        demoCommandParser();
        demoStatement(savings);
        demoInterestBearing(accounts, current);
        demoWithdrawRule(savings, fixedDeposit);
        demoInterfaces(savings, current, fixedDeposit);
        demoAnnotationValidation();
        demoTransfers();
        demoConcurrentDeposits();
        demoTransactionProcessor();
        demoProducerConsumer();
        demoDeadlockPrevention();
    }


    private static void demoPolymorphism(Account[] accounts) {
        System.out.println("\n" + SEP);
        System.out.println("Runtime Polymorphism — interestRate()");
        System.out.println(SEP);
        for (Account account : accounts) {
            System.out.printf("%-24s  %-10s  rate: %.1f%%%n",
                    account.getClass().getSimpleName(),
                    account.getOwnerName(),
                    account.interestRate());
        }
    }

    private static void demoPatternMatching(Account[] accounts, SavingsAccount savings) {
        System.out.println("\n" + SEP);
        System.out.println("Pattern-Matching instanceof");
        System.out.println(SEP);
        for (Account account : accounts) {
            if (account instanceof SavingsAccount sa) {
                System.out.println("SavingsAccount — owner: " + sa.getOwnerName());
                System.out.println("  Min balance : ₹" + sa.getMinBalance());
                System.out.println("  Balance     : ₹" + sa.getBalance());
                tryWithdraw(sa, 3500,
                        "  Withdraw ₹3500 (leaves ₹1500 >= min ₹1000)");
                tryWithdraw(sa, 600,
                        "  Withdraw ₹600  (would leave ₹900 < min ₹1000)");
            }
        }
        System.out.println("instanceof Account        : " + (savings instanceof Account));
        System.out.println("instanceof SavingsAccount : " + (savings instanceof SavingsAccount));
        System.out.println("instanceof Object         : " + (savings instanceof Object));
    }

    private static void demoWithdrawalRules(SavingsAccount savings,
                                             CurrentAccount current,
                                             FixedDepositAccount fixedDeposit) {
        System.out.println("\n" + SEP);
        System.out.println("Withdrawal Rules");
        System.out.println(SEP);

        System.out.println("SavingsAccount (min balance ₹1000)  balance: ₹" + savings.getBalance());
        tryWithdraw(savings,  400,   "  Withdraw ₹400               ");
        tryWithdraw(savings, 10000,  "  Withdraw ₹10000 (breach min)");

        System.out.println("CurrentAccount (overdraft ₹1000)   balance: ₹" + current.getBalance());
        tryWithdraw(current, 2500,  "  Withdraw ₹2500 (within limit) ");
        tryWithdraw(current,  600,  "  Withdraw ₹600  (exceeds limit)");
        tryWithdraw(current,  500,  "  Withdraw ₹500  (hits limit)   ");

        System.out.println("FixedDepositAccount (locked)        balance: ₹" + fixedDeposit.getBalance());
        tryWithdraw(fixedDeposit,    1, "  Withdraw ₹1   ");
        tryWithdraw(fixedDeposit, 5000, "  Withdraw ₹5000");

        System.out.println("Zero / negative guard:");
        tryWithdrawInvalid(savings, 0,   "  Withdraw ₹0   from savings");
        tryWithdrawInvalid(savings, -50, "  Withdraw ₹-50 from savings");
    }

    private static void demoInsufficientFunds() {
        System.out.println("\n" + SEP);
        System.out.println("Insufficient Funds");
        System.out.println(SEP);
        SavingsAccount demo = new SavingsAccount("Customer3", 5000, 0);
        tryDeposit(demo, 1000);
        tryWithdraw(demo, 2500, "Withdraw ₹2500");
        long bigWithdraw = 10000;
        System.out.println("Withdraw ₹" + bigWithdraw + " from " + demo.getOwnerName()
                + " (balance: ₹" + demo.getBalance() + ")");
        try {
            demo.withdraw(bigWithdraw);
            System.out.println("Result: success | balance: ₹" + demo.getBalance());
        } catch (InsufficientFundsException e) {
            System.out.println("Result: rejected — insufficient funds | balance: ₹" + demo.getBalance());
        } catch (InvalidAmountException e) {
            System.out.println("Result: rejected — invalid amount | balance: ₹" + demo.getBalance());
        }
    }

    private static void demoEquality(Account[] accounts, SavingsAccount savings, CurrentAccount current) {
        System.out.println("\n" + SEP);
        System.out.println("toString / equals / hashCode");
        System.out.println(SEP);
        for (Account account : accounts) {
            System.out.println(account);
        }
        System.out.println("savings.equals(current) : " + savings.equals(current));
        System.out.println("savings.equals(savings) : " + savings.equals(savings));
        System.out.println("savings.hashCode()      : " + savings.hashCode());
        System.out.println("current.hashCode()      : " + current.hashCode());
    }

    private static void demoCustomer() {
        System.out.println("\n" + SEP);
        System.out.println("Customer, Address & Cloning");
        System.out.println(SEP);
        Customer.Address address = new Customer.Address("123 Main Road", "Navsari", "396445");
        Customer original = new Customer("Rinkesh", "rinkesh@example.com", "9876543210", address);
        System.out.println("Original : ID=" + original.getCustomerId()
                + ", Name=" + original.getName()
                + ", Email=" + original.getEmail()
                + ", Mobile=" + original.getMobile());
        System.out.println("Address  : " + original.getAddress().getLine()
                + ", " + original.getAddress().getCity()
                + " - " + original.getAddress().getPincode());
        System.out.println("instanceof Customer: " + (original instanceof Customer));

        Customer cloned = original.clone();
        System.out.println("Cloned   : ID=" + cloned.getCustomerId()
                + ", Name=" + cloned.getName()
                + ", Email=" + cloned.getEmail()
                + ", Mobile=" + cloned.getMobile());
        System.out.println("Distinct references     : " + (original != cloned));
        System.out.println("Same customer ID        : " + original.getCustomerId().equals(cloned.getCustomerId()));
    }

    private static void demoValidator() {
        System.out.println("\n" + SEP);
        System.out.println("Input Validation");
        System.out.println(SEP);
        System.out.println("Mobile 9876543210 (valid)        : " + isValidMobile("9876543210"));
        System.out.println("Mobile 12345      (invalid)      : " + isValidMobile("12345"));
        System.out.println("Email  rinkesh@example.com       : " + Validator.isValidEmail("rinkesh@example.com"));
        System.out.println("Email  rinkesh@   (invalid)      : " + Validator.isValidEmail("rinkesh@"));
        System.out.println("PAN    ABCDE1234F (valid)        : " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("PAN    ABC123     (invalid)      : " + Validator.isValidPan("ABC123"));
        System.out.println("IFSC   SBIN0001234 (valid)       : " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("IFSC   SBIN123     (invalid)     : " + Validator.isValidIfsc("SBIN123"));
    }

    private static void demoCommandParser() {
        System.out.println("\n" + SEP);
        System.out.println("Command Parsing");
        System.out.println(SEP);
        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("Input  : \"DEPOSIT AC0001 500\"");
        System.out.println("Type   : " + command.type());
        System.out.println("Account: " + command.accountNumber());
        System.out.println("Amount : " + command.amount());
    }

    private static void demoStatement(SavingsAccount savings) {
        System.out.println("\n" + SEP);
        System.out.println("Account Statement");
        System.out.println(SEP);
        System.out.println(StatementFormatter.buildStatement(savings));
    }

    private static void demoInterestBearing(Account[] accounts, CurrentAccount current) {
        System.out.println("\n" + SEP);
        System.out.println("Yearly Interest (InterestBearing default method)");
        System.out.println(SEP);
        for (Account account : accounts) {
            System.out.printf("%-24s  balance: ₹%-8d  rate: %.1f%%  interest: ₹%.2f%n",
                    account.getClass().getSimpleName(),
                    account.getBalance(),
                    account.interestRate(),
                    account.yearlyInterest());
        }
    }

    private static void demoWithdrawRule(SavingsAccount savings, FixedDepositAccount fixedDeposit) {
        System.out.println("\n" + SEP);
        System.out.println("WithdrawRule — Anonymous Class & Lambda");
        System.out.println(SEP);
        WithdrawRule anonymousRule = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account != null && amount > 0 && account.canWithdraw(amount);
            }
        };

        WithdrawRule lambdaRule = (account, amount) ->
                account != null && amount > 0 && account.canWithdraw(amount);

        long allowed  = 50;
        long rejected = 1;

        System.out.printf("Anonymous — SavingsAccount,      withdraw ₹%d: allow=%b%n",
                allowed,  anonymousRule.allow(savings, allowed));
        System.out.printf("Anonymous — FixedDepositAccount, withdraw ₹%d: allow=%b%n",
                rejected, anonymousRule.allow(fixedDeposit, rejected));
        System.out.printf("Lambda    — SavingsAccount,      withdraw ₹%d: allow=%b%n",
                allowed,  lambdaRule.allow(savings, allowed));
        System.out.printf("Lambda    — FixedDepositAccount, withdraw ₹%d: allow=%b%n",
                rejected, lambdaRule.allow(fixedDeposit, rejected));
    }

    private static void demoInterfaces(SavingsAccount savings,
                                        CurrentAccount current,
                                        FixedDepositAccount fixedDeposit) {
        System.out.println("\n" + SEP);
        System.out.println("Interfaces — Transactable & Premium");
        System.out.println(SEP);
        System.out.println("savings      instanceof Transactable: " + (savings      instanceof model.Transactable));
        System.out.println("current      instanceof Transactable: " + (current      instanceof model.Transactable));
        System.out.println("fixedDeposit instanceof Transactable: " + (fixedDeposit instanceof model.Transactable));
        System.out.println("Premium (marker interface)          : " + Premium.class.getName());
    }

    private static void demoAnnotationValidation() {
        System.out.println("\n" + SEP);
        System.out.println("Annotation Validation (Reflection)");
        System.out.println(SEP);

        // Valid account — no errors
        CurrentAccount valid = new CurrentAccount("Alice", 1000, 500);
        String[] e1 = AnnotationValidator.validate(valid);
        System.out.println("Valid account (balance=1000, name<=50)          : "
                + (e1.length == 0 ? "PASS" : "FAIL — " + e1.length + " error(s)"));

        // Negative balance → @Positive violation
        CurrentAccount negBalance = new CurrentAccount("ValidationTest", -100, 1000);
        String[] e2 = AnnotationValidator.validate(negBalance);
        System.out.println("Negative balance (balance=-100)                 : "
                + (containsField(e2, "balance") ? "PASS  → " + e2[0] : "FAIL"));

        // Zero balance → strictly-positive violation
        SavingsAccount zeroBal = new SavingsAccount("ZeroTest", 0, 0);
        String[] e3 = AnnotationValidator.validate(zeroBal);
        System.out.println("Zero balance                                    : "
                + (containsField(e3, "balance") ? "PASS  → " + e3[0] : "FAIL"));

        // Name exactly 50 chars — no MaxLength error
        CurrentAccount exact50 = new CurrentAccount("A".repeat(50), 500, 200);
        String[] e4 = AnnotationValidator.validate(exact50);
        boolean noMaxLen = java.util.Arrays.stream(e4).noneMatch(e -> e.contains("must have at most"));
        System.out.println("Name exactly 50 chars (boundary — accepted)     : " + (noMaxLen ? "PASS" : "FAIL"));

        // Name 51 chars — MaxLength violation
        CurrentAccount long51 = new CurrentAccount("B".repeat(51), 500, 200);
        String[] e5 = AnnotationValidator.validate(long51);
        boolean hasMaxLen = java.util.Arrays.stream(e5).anyMatch(e -> e.contains("must have at most"));
        System.out.println("Name 51 chars (exceeds max — rejected)          : "
                + (hasMaxLen ? "PASS  → " + e5[0] : "FAIL"));

        // Both violations together
        CurrentAccount both = new CurrentAccount("B".repeat(55), -200, 1000);
        String[] e6 = AnnotationValidator.validate(both);
        System.out.println("Name>50 + negative balance (two violations)     : "
                + (e6.length == 2 ? "PASS  (" + e6.length + " errors)" : "FAIL — " + e6.length + " error(s)"));
        for (String err : e6) {
            System.out.println("  → " + err);
        }

        // Null object
        String[] e7 = AnnotationValidator.validate(null);
        System.out.println("Null object                                     : "
                + (e7.length == 1 ? "PASS  → " + e7[0] : "FAIL"));

        // Superclass hierarchy: annotations in Account found via subclass instance
        SavingsAccount negSav = new SavingsAccount("HierarchyTest", -500, 0);
        String[] e8a = AnnotationValidator.validate(negSav);
        System.out.println("SavingsAccount(-500) — Account.balance via hierarchy : "
                + (containsField(e8a, "balance") ? "PASS  → " + e8a[0] : "FAIL"));

        FixedDepositAccount negFD = new FixedDepositAccount("FDTest", -1);
        String[] e8b = AnnotationValidator.validate(negFD);
        System.out.println("FixedDepositAccount(-1) — Account.balance via hierarchy : "
                + (containsField(e8b, "balance") ? "PASS  → " + e8b[0] : "FAIL"));
    }

    private static void demoTransfers() {
        System.out.println("\n" + SEP);
        System.out.println("Transfers & Error Handling");
        System.out.println(SEP);

        demoSuccessfulTransfer();
        demoFailedTransfers();
        demoTryWithResources();
    }

    private static void demoSuccessfulTransfer() {
        System.out.println("\n[Transfer] Successful transfer:");
        SavingsAccount src  = new SavingsAccount("Alice", 5000, 0);
        SavingsAccount dest = new SavingsAccount("Bob",   1000, 0);
        long amount = 2000;

        long srcBefore  = src.getBalance();
        long destBefore = dest.getBalance();

        System.out.printf("  Before: %s balance=₹%d, %s balance=₹%d%n",
                src.getOwnerName(), srcBefore, dest.getOwnerName(), destBefore);
        System.out.println("  Transferring ₹" + amount + " from " + src.getOwnerName()
                + " → " + dest.getOwnerName());

        try {
            src.transfer(dest, amount);
            long srcAfter  = src.getBalance();
            long destAfter = dest.getBalance();
            System.out.printf("  After : %s balance=₹%d, %s balance=₹%d%n",
                    src.getOwnerName(), srcAfter, dest.getOwnerName(), destAfter);
            boolean correct = (srcAfter == srcBefore - amount) && (destAfter == destBefore + amount);
            System.out.println("  Balance check: " + (correct ? "PASS" : "FAIL"));
        } catch (BankException e) {
            System.out.println("  Unexpected failure: " + e.getMessage());
        }
    }

    private static void demoFailedTransfers() {
        System.out.println("\n[Transfer] Failed transfer — insufficient funds:");
        SavingsAccount poor   = new SavingsAccount("Charlie", 500, 0);
        SavingsAccount target = new SavingsAccount("Diana",  1000, 0);
        long poorBefore   = poor.getBalance();
        long targetBefore = target.getBalance();
        try {
            poor.transfer(target, 2000);
            System.out.println("  FAIL — expected InsufficientFundsException");
        } catch (InsufficientFundsException e) {
            System.out.println("  Rejected: " + e.getMessage());
            System.out.println("  Shortfall: ₹" + e.getShortfall());
            boolean unchanged = poor.getBalance() == poorBefore && target.getBalance() == targetBefore;
            System.out.println("  Balances unchanged: " + (unchanged ? "PASS" : "FAIL"));
        } catch (BankException e) {
            System.out.println("  Unexpected exception type: " + e.getMessage());
        }

        System.out.println("\n[Transfer] Failed transfer — null destination:");
        SavingsAccount any = new SavingsAccount("Eve", 3000, 0);
        long anyBefore = any.getBalance();
        try {
            any.transfer(null, 100);
            System.out.println("  FAIL — expected AccountNotFoundException");
        } catch (AccountNotFoundException e) {
            System.out.println("  Rejected: " + e.getMessage());
            System.out.println("  Balance unchanged: " + (any.getBalance() == anyBefore ? "PASS" : "FAIL"));
        } catch (BankException e) {
            System.out.println("  Unexpected exception type: " + e.getMessage());
        }

        System.out.println("\n[Transfer] Failed transfer — zero amount:");
        SavingsAccount from = new SavingsAccount("Frank", 2000, 0);
        SavingsAccount to   = new SavingsAccount("Grace", 2000, 0);
        long fromBefore = from.getBalance();
        long toBefore   = to.getBalance();
        try {
            from.transfer(to, 0);
            System.out.println("  FAIL — expected InvalidAmountException");
        } catch (InvalidAmountException e) {
            System.out.println("  Rejected: " + e.getMessage());
            boolean unchanged = from.getBalance() == fromBefore && to.getBalance() == toBefore;
            System.out.println("  Balances unchanged: " + (unchanged ? "PASS" : "FAIL"));
        } catch (BankException e) {
            System.out.println("  Unexpected exception type: " + e.getMessage());
        }

        System.out.println("\n[Transfer] Failed transfer — self-transfer:");
        SavingsAccount self = new SavingsAccount("Hank", 3000, 0);
        long selfBefore = self.getBalance();
        try {
            self.transfer(self, 500);
            System.out.println("  FAIL — expected BankException");
        } catch (BankException e) {
            System.out.println("  Rejected: " + e.getMessage());
            System.out.println("  Balance unchanged: " + (self.getBalance() == selfBefore ? "PASS" : "FAIL"));
        }

        System.out.println("\n[Transfer] Failed transfer — destination overflow:");
        SavingsAccount richSrc  = new SavingsAccount("Ivan",   1000, 0);
        CurrentAccount nearMax = new CurrentAccount("Jane", 0, 0);
        long safeDeposit = Long.MAX_VALUE - 500;
        long richBefore   = richSrc.getBalance();
        long nearMaxBefore = nearMax.getBalance();
        try {
            nearMax.deposit(safeDeposit);
            nearMaxBefore = nearMax.getBalance();
            richSrc.transfer(nearMax, 1000);
            System.out.println("  FAIL — expected InvalidAmountException for overflow");
        } catch (InvalidAmountException e) {
            System.out.println("  Rejected (overflow): " + e.getMessage());
            boolean unchanged = richSrc.getBalance() == richBefore
                    && nearMax.getBalance() == nearMaxBefore;
            System.out.println("  Balances unchanged: " + (unchanged ? "PASS" : "FAIL"));
        } catch (BankException e) {
            System.out.println("  Caught BankException: " + e.getMessage());
        }
    }

    private static void demoTryWithResources() {
        System.out.println("\n[Try-with-Resources] DemoResource:");
        try (DemoResource resource = new DemoResource()) {
            resource.use();
        } catch (Exception e) {
            System.out.println("Resource demonstration failed: " + e.getMessage());
        }
    }

    private static void demoConcurrentDeposits() {
        System.out.println("\n" + SEP);
        System.out.println("Concurrent Deposits — Synchronization Demo");
        System.out.println(SEP);

        final int  THREAD_COUNT = 10;
        final int  DEPOSITS_PER_THREAD = 10_000;
        final long DEPOSIT_AMOUNT = 1L;
        final long INITIAL_BALANCE = 0L;

        SavingsAccount shared = new SavingsAccount("SharedAccount", INITIAL_BALANCE, 0);

        Thread[] workers = new Thread[THREAD_COUNT];
        for (int i = 0; i < THREAD_COUNT; i++) {
            workers[i] = new Thread(
                    new AccountWorker(shared, DEPOSITS_PER_THREAD, DEPOSIT_AMOUNT),
                    "BankWorker-" + (i + 1));
        }

        for (Thread t : workers) {
            t.start();
        }

        for (Thread t : workers) {
            try {
                t.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Main thread interrupted while waiting for workers.");
            }
        }

        long expected = INITIAL_BALANCE + (long) THREAD_COUNT * DEPOSITS_PER_THREAD * DEPOSIT_AMOUNT;
        long actual   = shared.getBalance();

        System.out.println("Threads       : " + THREAD_COUNT);
        System.out.println("Deposits/thread: " + DEPOSITS_PER_THREAD);
        System.out.println("Amount/deposit : \u20b9" + DEPOSIT_AMOUNT);
        System.out.println("Expected balance: \u20b9" + expected);
        System.out.println("Actual   balance: \u20b9" + actual);
        System.out.println("Result          : " + (actual == expected ? "PASS" : "FAIL"));
    }


    /** Attempts a withdrawal; prints success or the appropriate rejection message. */
    private static void tryWithdraw(Account account, long amount, String label) {
        try {
            boolean ok = account.withdraw(amount);
            System.out.println(label + " → " + ok + " | balance: ₹" + account.getBalance());
        } catch (InsufficientFundsException e) {
            System.out.println(label + " → rejected (insufficient funds, shortfall ₹"
                    + e.getShortfall() + ") | balance: ₹" + account.getBalance());
        } catch (InvalidAmountException e) {
            System.out.println(label + " → rejected (invalid amount) | balance: ₹"
                    + account.getBalance());
        }
    }

    /** For zero/negative withdrawal demonstrations — expects InvalidAmountException. */
    private static void tryWithdrawInvalid(Account account, long amount, String label) {
        long before = account.getBalance();
        try {
            account.withdraw(amount);
            System.out.println(label + " → false");
        } catch (InvalidAmountException e) {
            System.out.println(label + " → rejected (invalid amount) | balance unchanged: ₹"
                    + account.getBalance());
        } catch (InsufficientFundsException e) {
            System.out.println(label + " → rejected (insufficient) | balance: ₹" + account.getBalance());
        }
    }

    /** Attempts a deposit silently; used in demonstration setup. */
    private static void tryDeposit(Account account, long amount) {
        try {
            account.deposit(amount);
        } catch (InvalidAmountException e) {
            System.out.println("Deposit ₹" + amount + " rejected: " + e.getMessage());
        }
    }

    private static boolean containsField(String[] errors, String fieldName) {
        for (String err : errors) {
            if (err.startsWith(fieldName + " ")) {
                return true;
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Practical 10 demonstrations
    // -------------------------------------------------------------------------

    private static void demoTransactionProcessor() {
        System.out.println("\n" + SEP);
        System.out.println("Transaction Processor (Fixed Thread Pool)");
        System.out.println(SEP);

        final long INITIAL_BALANCE    = 10_000L;
        final int  DEPOSIT_TASKS      = 100;
        final long DEPOSIT_AMOUNT     = 10L;
        final int  WITHDRAWAL_TASKS   = 50;
        final long WITHDRAWAL_AMOUNT  = 5L;

        SavingsAccount shared = new SavingsAccount("PoolDemo", INITIAL_BALANCE, 0);
        TransactionProcessor processor = new TransactionProcessor();

        for (int i = 0; i < DEPOSIT_TASKS; i++) {
            processor.submit(new AccountTransactionTask(shared, TransactionType.DEPOSIT, DEPOSIT_AMOUNT));
        }
        for (int i = 0; i < WITHDRAWAL_TASKS; i++) {
            processor.submit(new AccountTransactionTask(shared, TransactionType.WITHDRAW, WITHDRAWAL_AMOUNT));
        }

        processor.stop();   // waits for all tasks to complete

        long expected = INITIAL_BALANCE
                + (long) DEPOSIT_TASKS    * DEPOSIT_AMOUNT
                - (long) WITHDRAWAL_TASKS * WITHDRAWAL_AMOUNT;
        long actual   = shared.getBalance();

        System.out.println("Thread pool size  : 4");
        System.out.println("Deposit  tasks    : " + DEPOSIT_TASKS + " × ₹" + DEPOSIT_AMOUNT);
        System.out.println("Withdrawal tasks  : " + WITHDRAWAL_TASKS + " × ₹" + WITHDRAWAL_AMOUNT);
        System.out.println("Expected balance  : ₹" + expected);
        System.out.println("Actual   balance  : ₹" + actual);
        System.out.println("Result            : " + (actual == expected ? "PASS" : "FAIL"));
    }

    private static void demoProducerConsumer() {
        System.out.println("\n" + SEP);
        System.out.println("Producer-Consumer (wait/notify)");
        System.out.println(SEP);

        final int  DEPOSIT_COUNT     = 10;
        final long DEPOSIT_AMT       = 200L;
        final int  WITHDRAW_COUNT    = 4;
        final long WITHDRAW_AMT      = 150L;
        final long INITIAL_BALANCE   = 1_000L;

        SavingsAccount shared = new SavingsAccount("PCDemo", INITIAL_BALANCE, 0);
        TransactionBuffer buffer = new TransactionBuffer();

        long expectedBalance = INITIAL_BALANCE
                + (long) DEPOSIT_COUNT  * DEPOSIT_AMT
                - (long) WITHDRAW_COUNT * WITHDRAW_AMT;

        Thread producer = new Thread(() -> {
            System.out.println("[Producer] started.");
            try {
                for (int i = 0; i < DEPOSIT_COUNT; i++) {
                    buffer.produce(TransactionType.DEPOSIT, DEPOSIT_AMT);
                }
                for (int i = 0; i < WITHDRAW_COUNT; i++) {
                    buffer.produce(TransactionType.WITHDRAW, WITHDRAW_AMT);
                }
                System.out.println("[Producer] produced " + (DEPOSIT_COUNT + WITHDRAW_COUNT) + " transactions.");
                buffer.producePoison();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Producer] interrupted.");
            }
        }, "PC-Producer");

        Thread consumer = new Thread(() -> {
            System.out.println("[Consumer] started.");
            int processed = 0;
            try {
                while (true) {
                    long[] item = buffer.consume();
                    if (item[1] == TransactionBuffer.POISON_AMOUNT) {
                        break;
                    }
                    TransactionType type = TransactionType.values()[(int) item[0]];
                    long amount = item[1];
                    try {
                        if (type == TransactionType.DEPOSIT) {
                            shared.deposit(amount);
                        } else {
                            shared.withdraw(amount);
                        }
                        processed++;
                    } catch (exception.BankException e) {
                        System.err.println("[Consumer] transaction failed: " + e.getMessage());
                    }
                }
                System.out.println("[Consumer] processed " + processed + " transactions.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("[Consumer] interrupted.");
            }
        }, "PC-Consumer");

        producer.start();
        consumer.start();

        try {
            producer.join();
            consumer.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Main thread interrupted while joining producer/consumer.");
        }

        long actual = shared.getBalance();
        System.out.println("Expected balance  : ₹" + expectedBalance);
        System.out.println("Actual   balance  : ₹" + actual);
        System.out.println("Result            : " + (actual == expectedBalance ? "PASS" : "FAIL"));
    }

    private static void demoDeadlockPrevention() {
        System.out.println("\n" + SEP);
        System.out.println("Deadlock Prevention (Ordered Locks)");
        System.out.println(SEP);

        // --- Part A: demonstrate deadlock with opposite lock ordering ---
        SavingsAccount acctA = new SavingsAccount("DeadlockA", 5_000, 0);
        SavingsAccount acctB = new SavingsAccount("DeadlockB", 5_000, 0);

        CountDownLatch bothHoldFirst = new CountDownLatch(2);
        boolean deadlockDetected = false;

        // t1: locks acctA then tries acctB
        Thread t1 = new Thread(() -> {
            synchronized (acctA) {
                bothHoldFirst.countDown();
                try { bothHoldFirst.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                synchronized (acctB) { /* never reached in deadlock */ }
            }
        }, "DeadlockThread-1");

        // t2: locks acctB then tries acctA
        Thread t2 = new Thread(() -> {
            synchronized (acctB) {
                bothHoldFirst.countDown();
                try { bothHoldFirst.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                synchronized (acctA) { /* never reached in deadlock */ }
            }
        }, "DeadlockThread-2");

        t1.setDaemon(true);
        t2.setDaemon(true);
        t1.start();
        t2.start();

        // Give threads time to reach the deadlock state
        try { Thread.sleep(400); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }

        ThreadMXBean mxBean = ManagementFactory.getThreadMXBean();
        long[] deadlocked = mxBean.findDeadlockedThreads();
        if (deadlocked != null && deadlocked.length >= 2) {
            deadlockDetected = true;
        }

        System.out.println("Unsafe lock ordering deadlock detected : " + (deadlockDetected ? "YES" : "NO (timing-dependent)"));

        // Daemon threads will be killed when the JVM exits; no cleanup needed.

        // --- Part B: fix with ordered locks — repeated opposing transfers ---
        final long BALANCE_EACH  = 50_000L;
        final long TRANSFER_AMT  = 10L;
        final int  ITERATIONS    = 5_000;

        SavingsAccount ordA = new SavingsAccount("OrderedA", BALANCE_EACH, 0);
        SavingsAccount ordB = new SavingsAccount("OrderedB", BALANCE_EACH, 0);

        long combinedBefore = ordA.getBalance() + ordB.getBalance();

        boolean[] transferError = { false };

        Thread fa = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try {
                    ordA.transfer(ordB, TRANSFER_AMT);
                } catch (exception.BankException e) {
                    // Insufficient funds during opposing transfer is benign;
                    // a genuine unexpected exception is flagged.
                    if (!(e instanceof exception.InsufficientFundsException)) {
                        transferError[0] = true;
                        System.err.println("[OrderedTransfer-A] unexpected: " + e.getMessage());
                    }
                }
            }
        }, "OrderedTransfer-A");

        Thread fb = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                try {
                    ordB.transfer(ordA, TRANSFER_AMT);
                } catch (exception.BankException e) {
                    if (!(e instanceof exception.InsufficientFundsException)) {
                        transferError[0] = true;
                        System.err.println("[OrderedTransfer-B] unexpected: " + e.getMessage());
                    }
                }
            }
        }, "OrderedTransfer-B");

        fa.start();
        fb.start();

        try {
            fa.join(10_000);
            fb.join(10_000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean bothFinished = !fa.isAlive() && !fb.isAlive();
        long combinedAfter   = ordA.getBalance() + ordB.getBalance();
        boolean sumPreserved = (combinedAfter == combinedBefore);

        System.out.println("Ordered-lock transfer threads finished : " + (bothFinished ? "YES" : "NO (still alive — possible deadlock)"));
        System.out.println("Combined balance preserved             : " + (sumPreserved ? "PASS (₹" + combinedAfter + ")" : "FAIL (₹" + combinedAfter + ")"));
        System.out.println("No unexpected transfer exception       : " + (!transferError[0] ? "PASS" : "FAIL"));
        System.out.println("Deadlock prevention result            : " + (bothFinished && sumPreserved && !transferError[0] ? "PASS" : "FAIL"));
    }
}
