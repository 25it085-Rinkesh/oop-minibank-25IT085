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

// Static import for one meaningful validator method (Practical 6 — Requirement 6).
import static util.Validator.isValidMobile;

public class MiniBank {

    public static void main(String[] args) {

        BankInfo bank = new BankInfo("MiniBank", "Navsari");

        System.out.println("==================================");
        System.out.println("       WELCOME TO MINIBANK");
        System.out.println("==================================");
        System.out.println("Bank Name : " + bank.name());
        System.out.println("Branch    : " + bank.branch());

        Scanner sc = new Scanner(System.in);

        int choice = -1;

        do {
            System.out.println("\n========== MENU ==========");
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

        // ----------------------------------------------------------------
        // PRACTICAL 5 — ABSTRACT ACCOUNT, INHERITANCE, RUNTIME POLYMORPHISM
        // ----------------------------------------------------------------

        // --- A. Create one of each concrete subclass ---
        // SavingsAccount: balance ₹5000, minimum balance ₹1000
        SavingsAccount savings = new SavingsAccount("Rinkesh", 5000, 1000);

        // CurrentAccount: balance ₹2000, overdraft limit ₹1000
        CurrentAccount current = new CurrentAccount("Priya", 2000, 1000);

        // FixedDepositAccount: balance ₹10000
        FixedDepositAccount fixedDeposit = new FixedDepositAccount("Arjun", 10000);

        // Store all three in a single Account[] — runtime polymorphism
        Account[] accounts = { savings, current, fixedDeposit };

        // --- B. Demonstrate interestRate() through Account references ---
        System.out.println("\n===== RUNTIME POLYMORPHISM — interestRate() =====");
        for (Account account : accounts) {
            System.out.printf("Account: %-8s | Owner: %-10s | Type: %-22s | Interest Rate: %.1f%%%n",
                    account.getAccountNumber(),
                    account.getOwnerName(),
                    account.getClass().getSimpleName(),
                    account.interestRate());
        }

        // --- C. Pattern-matching instanceof for SavingsAccount ---
        System.out.println("\n===== PATTERN-MATCHING instanceof =====");
        for (Account account : accounts) {
            if (account instanceof SavingsAccount sa) {
                System.out.println("SavingsAccount detected for owner: " + sa.getOwnerName());
                System.out.println("  Minimum balance required : ₹" + sa.getMinBalance());
                System.out.println("  Current balance          : ₹" + sa.getBalance());
                System.out.println("  Withdraw ₹3500 (leaves ₹1500 >= min ₹1000): "
                        + sa.withdraw(3500)
                        + " | Balance after: ₹" + sa.getBalance());
                System.out.println("  Withdraw ₹600 (would leave ₹900 < min ₹1000): "
                        + sa.withdraw(600)
                        + " | Balance after: ₹" + sa.getBalance());
            }
        }

        // --- D. Subclass-specific withdrawal rules ---
        System.out.println("\n===== WITHDRAWAL RULES =====");

        System.out.println("\n-- SavingsAccount (min balance ₹1000) --");
        System.out.println("Balance: ₹" + savings.getBalance());
        boolean r1 = savings.withdraw(400);
        System.out.println("Withdraw ₹400  → success: " + r1 + " | Balance: ₹" + savings.getBalance());
        boolean r2 = savings.withdraw(10000);
        System.out.println("Withdraw ₹10000 (would breach min) → success: " + r2 + " | Balance: ₹" + savings.getBalance());

        System.out.println("\n-- CurrentAccount (overdraft limit ₹1000) --");
        System.out.println("Balance: ₹" + current.getBalance());
        boolean r3 = current.withdraw(2500);
        System.out.println("Withdraw ₹2500 (within limit, leaves ₹-500) → success: " + r3 + " | Balance: ₹" + current.getBalance());
        boolean r4 = current.withdraw(600);
        System.out.println("Withdraw ₹600  (exceeds limit)               → success: " + r4 + " | Balance: ₹" + current.getBalance());
        boolean r5 = current.withdraw(500);
        System.out.println("Withdraw ₹500  (exactly hits limit)          → success: " + r5 + " | Balance: ₹" + current.getBalance());

        System.out.println("\n-- FixedDepositAccount (locked) --");
        System.out.println("Balance: ₹" + fixedDeposit.getBalance());
        boolean r6 = fixedDeposit.withdraw(1);
        System.out.println("Withdraw ₹1    (any amount rejected) → success: " + r6 + " | Balance: ₹" + fixedDeposit.getBalance());
        boolean r7 = fixedDeposit.withdraw(5000);
        System.out.println("Withdraw ₹5000 (any amount rejected) → success: " + r7 + " | Balance: ₹" + fixedDeposit.getBalance());

        System.out.println("\n-- Zero / negative withdrawal guard --");
        System.out.println("Withdraw ₹0   from savings → success: " + savings.withdraw(0));
        System.out.println("Withdraw ₹-50 from savings → success: " + savings.withdraw(-50));

        // ----------------------------------------------------------------
        // PRACTICALS 1–4 DEMONSTRATIONS (preserved)
        // ----------------------------------------------------------------

        System.out.println("\n===== INSUFFICIENT-FUNDS EXAMPLE =====");
        SavingsAccount demo = new SavingsAccount("Customer3", 5000, 0);
        demo.deposit(1000);
        demo.withdraw(2500);
        long bigWithdraw = 10000;
        System.out.println("Attempting to withdraw ₹" + bigWithdraw + " from " + demo.getOwnerName()
                + "'s account (Balance: ₹" + demo.getBalance() + ")...");
        boolean demoSuccess = demo.withdraw(bigWithdraw);
        System.out.println("Withdrawal successful: " + demoSuccess);
        if (!demoSuccess) {
            System.out.println("Transaction rejected: Insufficient funds. Balance remains: ₹" + demo.getBalance());
        }

        System.out.println("\n===== ACCOUNT DETAILS (toString()) =====");
        for (Account account : accounts) {
            System.out.println(account.toString());
        }

        System.out.println("\n===== ACCOUNT EQUALITY (equals() & hashCode()) =====");
        System.out.println("Comparing savings with current : " + savings.equals(current));
        System.out.println("Comparing savings with itself  : " + savings.equals(savings));
        System.out.println("savings hashCode  : " + savings.hashCode());
        System.out.println("current hashCode  : " + current.hashCode());

        System.out.println("\n===== TYPE CHECKING (instanceof) =====");
        Object obj = savings;
        System.out.println("obj instanceof Account        : " + (obj instanceof Account));
        System.out.println("savings instanceof Object     : " + (savings instanceof Object));
        System.out.println("savings instanceof SavingsAccount : " + (savings instanceof SavingsAccount));

        System.out.println("\n===== CUSTOMER, ADDRESS & CLONING =====");
        Customer.Address address = new Customer.Address("123 Main Road", "Navsari", "396445");
        Customer originalCustomer = new Customer("Rinkesh", "rinkesh@example.com", "9876543210", address);

        System.out.println("Original Customer: ID=" + originalCustomer.getCustomerId()
                + ", Name=" + originalCustomer.getName()
                + ", Email=" + originalCustomer.getEmail()
                + ", Mobile=" + originalCustomer.getMobile());
        System.out.println("Address: " + originalCustomer.getAddress().getLine()
                + ", " + originalCustomer.getAddress().getCity()
                + " - " + originalCustomer.getAddress().getPincode());
        System.out.println("originalCustomer instanceof Customer: " + (originalCustomer instanceof Customer));

        Customer clonedCustomer = originalCustomer.clone();
        System.out.println("\nCloned Customer:   ID=" + clonedCustomer.getCustomerId()
                + ", Name=" + clonedCustomer.getName()
                + ", Email=" + clonedCustomer.getEmail()
                + ", Mobile=" + clonedCustomer.getMobile());
        System.out.println("Distinct references (original != clone): " + (originalCustomer != clonedCustomer));
        System.out.println("Same Customer ID:                      " + originalCustomer.getCustomerId().equals(clonedCustomer.getCustomerId()));

        // Static import used for isValidMobile; other Validator calls remain qualified.
        System.out.println("\n===== INPUT VALIDATION (Validator) =====");
        System.out.println("Valid Mobile (9876543210):         " + isValidMobile("9876543210"));
        System.out.println("Invalid Mobile (12345):            " + isValidMobile("12345"));
        System.out.println("Valid Email (rinkesh@example.com): " + Validator.isValidEmail("rinkesh@example.com"));
        System.out.println("Invalid Email (rinkesh@):          " + Validator.isValidEmail("rinkesh@"));
        System.out.println("Valid PAN (ABCDE1234F):            " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("Invalid PAN (ABC123):              " + Validator.isValidPan("ABC123"));
        System.out.println("Valid IFSC (SBIN0001234):          " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("Invalid IFSC (SBIN123):            " + Validator.isValidIfsc("SBIN123"));

        System.out.println("\n===== COMMAND PARSING (CommandParser) =====");
        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("Transaction Type: " + command.type());
        System.out.println("Account Number:   " + command.accountNumber());
        System.out.println("Amount:           " + command.amount());

        System.out.println("\n===== ACCOUNT STATEMENT (StatementFormatter) =====");
        String statement = StatementFormatter.buildStatement(savings);
        System.out.println(statement);

        // ----------------------------------------------------------------
        // PRACTICAL 6 — INTERFACES, POLYMORPHISM, PACKAGES
        // ----------------------------------------------------------------

        // --- Requirement 2: yearlyInterest() via InterestBearing default method ---
        System.out.println("\n===== YEARLY INTEREST (InterestBearing default method) =====");
        // Recreate accounts with predictable balances for clear interest output.
        // Use the same array already populated; balances have changed due to
        // withdrawal demonstrations above, so we display actual current balances.
        for (Account account : accounts) {
            System.out.printf("Type: %-22s | Owner: %-8s | Balance: ₹%-8d | Rate: %.1f%% | Yearly Interest: ₹%.2f%n",
                    account.getClass().getSimpleName(),
                    account.getOwnerName(),
                    account.getBalance(),
                    account.interestRate(),
                    account.yearlyInterest());
        }
        // CurrentAccount balance is negative after overdraft demo; verify interest on negative balance.
        System.out.println("Note: CurrentAccount has a negative balance from the overdraft demonstration.");
        System.out.println("      yearlyInterest() correctly returns: " + current.yearlyInterest()
                + " (0.0% rate means no interest regardless of balance).");

        // --- Requirement 3: WithdrawRule — anonymous class ---
        System.out.println("\n===== WITHDRAW RULE — Anonymous Class =====");
        WithdrawRule anonymousRule = new WithdrawRule() {
            @Override
            public boolean allow(Account account, long amount) {
                return account != null
                        && amount > 0
                        && account.canWithdraw(amount);
            }
        };

        // SavingsAccount after demonstrations: balance is ₹100 (5000-3500-400 = 1100, then -400 withdrawn above)
        // Use the fixed-deposit account to show a rejected case (always locked).
        long testAmountAllowed = 50;
        long testAmountRejected = 1;
        System.out.printf("Anonymous rule — SavingsAccount, withdraw ₹%d: allow = %b | Balance: ₹%d%n",
                testAmountAllowed, anonymousRule.allow(savings, testAmountAllowed), savings.getBalance());
        System.out.printf("Anonymous rule — FixedDepositAccount, withdraw ₹%d: allow = %b | Balance: ₹%d%n",
                testAmountRejected, anonymousRule.allow(fixedDeposit, testAmountRejected), fixedDeposit.getBalance());
        // Confirm balance is unchanged after allow() calls.
        System.out.println("SavingsAccount balance after allow() calls (must be unchanged): ₹" + savings.getBalance());

        // --- Requirement 3: WithdrawRule — lambda expression ---
        System.out.println("\n===== WITHDRAW RULE — Lambda Expression =====");
        WithdrawRule lambdaRule = (account, amount) ->
                account != null
                        && amount > 0
                        && account.canWithdraw(amount);

        System.out.printf("Lambda rule — SavingsAccount, withdraw ₹%d: allow = %b | Balance: ₹%d%n",
                testAmountAllowed, lambdaRule.allow(savings, testAmountAllowed), savings.getBalance());
        System.out.printf("Lambda rule — FixedDepositAccount, withdraw ₹%d: allow = %b | Balance: ₹%d%n",
                testAmountRejected, lambdaRule.allow(fixedDeposit, testAmountRejected), fixedDeposit.getBalance());
        System.out.println("SavingsAccount balance after lambda allow() calls (must be unchanged): ₹" + savings.getBalance());

        // --- Requirement 4: Premium marker interface ---
        System.out.println("\n===== PREMIUM MARKER INTERFACE =====");
        System.out.println("Premium is declared as a marker interface in package model.");
        System.out.println("It has no methods and no fields.");
        System.out.println("Premium interface exists: " + Premium.class.getName());

        // --- Practical 6: Transactable verification ---
        System.out.println("\n===== TRANSACTABLE INTERFACE VERIFICATION =====");
        // Account implements Transactable; all three subclasses inherit the behaviour.
        System.out.println("savings  instanceof Transactable : " + (savings instanceof model.Transactable));
        System.out.println("current  instanceof Transactable : " + (current instanceof model.Transactable));
        System.out.println("fixedDeposit instanceof Transactable : " + (fixedDeposit instanceof model.Transactable));

        // ----------------------------------------------------------------
        // PRACTICAL 7 — CUSTOM ANNOTATIONS, REFLECTION, RUNTIME VALIDATION
        // ----------------------------------------------------------------

        System.out.println("\n===== ANNOTATION VALIDATION (Reflection) =====");

        // --- Test 1: Valid account — no errors expected ---
        System.out.println("\n-- Test 1: Valid account (positive balance, name within 50 chars) --");
        CurrentAccount validAccount = new CurrentAccount("Alice", 1000, 500);
        String[] noErrors = AnnotationValidator.validate(validAccount);
        System.out.println("Errors found: " + noErrors.length + (noErrors.length == 0 ? " (PASS — no violations)" : ""));

        // --- Test 2: Negative balance triggers @Positive violation ---
        System.out.println("\n-- Test 2: Negative balance — expects balance @Positive error --");
        CurrentAccount invalidAccount = new CurrentAccount("ValidationTest", -100, 1000);
        String[] balanceErrors = AnnotationValidator.validate(invalidAccount);
        System.out.println("Errors found: " + balanceErrors.length);
        for (String err : balanceErrors) {
            System.out.println("  [ERROR] " + err);
        }

        // --- Test 3: Zero balance triggers @Positive violation ---
        System.out.println("\n-- Test 3: Zero balance — strictly-positive rule requires balance > 0 --");
        SavingsAccount zeroBalance = new SavingsAccount("ZeroTest", 0, 0);
        String[] zeroErrors = AnnotationValidator.validate(zeroBalance);
        System.out.println("Errors found: " + zeroErrors.length);
        for (String err : zeroErrors) {
            System.out.println("  [ERROR] " + err);
        }

        // --- Test 4: Name exactly at 50 chars — no @MaxLength error ---
        System.out.println("\n-- Test 4: Owner name exactly 50 characters — no MaxLength error --");
        String name50 = "A".repeat(50);
        CurrentAccount exactLength = new CurrentAccount(name50, 500, 200);
        String[] exactErrors = AnnotationValidator.validate(exactLength);
        // filter to MaxLength errors only for clarity
        long maxLenViolations = java.util.Arrays.stream(exactErrors)
                .filter(e -> e.contains("must have at most")).count();
        System.out.println("MaxLength violations: " + maxLenViolations + (maxLenViolations == 0 ? " (PASS)" : " (FAIL)"));

        // --- Test 5: Name longer than 50 chars triggers @MaxLength error ---
        System.out.println("\n-- Test 5: Owner name 51 characters — expects MaxLength error --");
        String name51 = "B".repeat(51);
        CurrentAccount longName = new CurrentAccount(name51, 500, 200);
        String[] longNameErrors = AnnotationValidator.validate(longName);
        System.out.println("Errors found: " + longNameErrors.length);
        for (String err : longNameErrors) {
            System.out.println("  [ERROR] " + err);
        }

        // --- Test 6: Both negative balance AND overlong name ---
        System.out.println("\n-- Test 6: Negative balance + name > 50 chars — expects two errors --");
        CurrentAccount bothBad = new CurrentAccount("B".repeat(55), -200, 1000);
        String[] bothErrors = AnnotationValidator.validate(bothBad);
        System.out.println("Errors found: " + bothErrors.length);
        for (String err : bothErrors) {
            System.out.println("  [ERROR] " + err);
        }

        // --- Test 7: Null object ---
        System.out.println("\n-- Test 7: null object --");
        String[] nullErrors = AnnotationValidator.validate(null);
        System.out.println("Errors found: " + nullErrors.length);
        for (String err : nullErrors) {
            System.out.println("  [ERROR] " + err);
        }

        // --- Test 8: Superclass hierarchy — SavingsAccount and FixedDepositAccount ---
        System.out.println("\n-- Test 8: Superclass hierarchy — @Positive on Account.balance found via SavingsAccount and FixedDepositAccount --");
        SavingsAccount negSavings = new SavingsAccount("HierarchyTest", -500, 0);
        String[] savErrors = AnnotationValidator.validate(negSavings);
        System.out.println("SavingsAccount(-500) errors: " + savErrors.length);
        for (String err : savErrors) {
            System.out.println("  [ERROR] " + err);
        }
        FixedDepositAccount negFD = new FixedDepositAccount("FDTest", -1);
        String[] fdErrors = AnnotationValidator.validate(negFD);
        System.out.println("FixedDepositAccount(-1) errors: " + fdErrors.length);
        for (String err : fdErrors) {
            System.out.println("  [ERROR] " + err);
        }

        System.out.println("\n===== PRACTICAL 7 COMPLETE =====");
    }
}
