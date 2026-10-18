package service;

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

import java.util.Scanner;

import static util.Validator.isValidMobile;

public class MiniBank {

    private static final String SEP = "----------------------------------------";

    public static void main(String[] args) {

        runDemonstrations();

        BankInfo bank = new BankInfo("MiniBank", "Navsari");

        System.out.println("\n==================================");
        System.out.println("       WELCOME TO MINIBANK");
        System.out.println("==================================");
        System.out.println("Bank Name : " + bank.name());
        System.out.println("Branch    : " + bank.branch());

        Scanner sc = new Scanner(System.in);
        int choice = -1;

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

        sc.close();
    }

    // -----------------------------------------------------------------------
    // Demonstration methods (Practicals 1 – 7)
    // -----------------------------------------------------------------------

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
                System.out.println("  Withdraw ₹3500 (leaves ₹1500 >= min ₹1000): "
                        + sa.withdraw(3500) + " | balance: ₹" + sa.getBalance());
                System.out.println("  Withdraw ₹600  (would leave ₹900 < min ₹1000): "
                        + sa.withdraw(600) + " | balance: ₹" + sa.getBalance());
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
        System.out.println("  Withdraw ₹400                → " + savings.withdraw(400)  + " | balance: ₹" + savings.getBalance());
        System.out.println("  Withdraw ₹10000 (breach min) → " + savings.withdraw(10000) + " | balance: ₹" + savings.getBalance());

        System.out.println("CurrentAccount (overdraft ₹1000)   balance: ₹" + current.getBalance());
        System.out.println("  Withdraw ₹2500 (within limit)  → " + current.withdraw(2500) + " | balance: ₹" + current.getBalance());
        System.out.println("  Withdraw ₹600  (exceeds limit) → " + current.withdraw(600)  + " | balance: ₹" + current.getBalance());
        System.out.println("  Withdraw ₹500  (hits limit)    → " + current.withdraw(500)  + " | balance: ₹" + current.getBalance());

        System.out.println("FixedDepositAccount (locked)        balance: ₹" + fixedDeposit.getBalance());
        System.out.println("  Withdraw ₹1    → " + fixedDeposit.withdraw(1)    + " | balance: ₹" + fixedDeposit.getBalance());
        System.out.println("  Withdraw ₹5000 → " + fixedDeposit.withdraw(5000) + " | balance: ₹" + fixedDeposit.getBalance());

        System.out.println("Zero / negative guard:");
        System.out.println("  Withdraw ₹0   from savings → " + savings.withdraw(0));
        System.out.println("  Withdraw ₹-50 from savings → " + savings.withdraw(-50));
    }

    private static void demoInsufficientFunds() {
        System.out.println("\n" + SEP);
        System.out.println("Insufficient Funds");
        System.out.println(SEP);
        SavingsAccount demo = new SavingsAccount("Customer3", 5000, 0);
        demo.deposit(1000);
        demo.withdraw(2500);
        long bigWithdraw = 10000;
        System.out.println("Withdraw ₹" + bigWithdraw + " from " + demo.getOwnerName()
                + " (balance: ₹" + demo.getBalance() + ")");
        boolean ok = demo.withdraw(bigWithdraw);
        System.out.println("Result: " + (ok ? "success" : "rejected — insufficient funds")
                + " | balance: ₹" + demo.getBalance());
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

    private static boolean containsField(String[] errors, String fieldName) {
        for (String err : errors) {
            if (err.startsWith(fieldName + " ")) {
                return true;
            }
        }
        return false;
    }
}
