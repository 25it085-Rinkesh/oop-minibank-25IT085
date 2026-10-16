import java.util.Scanner;

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

        // SavingsAccount — already shown above via pattern matching; show rejected case clearly
        System.out.println("\n-- SavingsAccount (min balance ₹1000) --");
        System.out.println("Balance: ₹" + savings.getBalance());
        boolean r1 = savings.withdraw(400);
        System.out.println("Withdraw ₹400  → success: " + r1 + " | Balance: ₹" + savings.getBalance());
        boolean r2 = savings.withdraw(10000);
        System.out.println("Withdraw ₹10000 (would breach min) → success: " + r2 + " | Balance: ₹" + savings.getBalance());

        // CurrentAccount
        System.out.println("\n-- CurrentAccount (overdraft limit ₹1000) --");
        System.out.println("Balance: ₹" + current.getBalance());
        boolean r3 = current.withdraw(2500); // 2000 + 1000 limit = max 3000 total; leaves -500
        System.out.println("Withdraw ₹2500 (within limit, leaves ₹-500) → success: " + r3 + " | Balance: ₹" + current.getBalance());
        boolean r4 = current.withdraw(600);  // balance -500, limit 1000 → max further = 500; 600 > 500
        System.out.println("Withdraw ₹600  (exceeds limit)               → success: " + r4 + " | Balance: ₹" + current.getBalance());
        boolean r5 = current.withdraw(500);  // exactly exhausts overdraft
        System.out.println("Withdraw ₹500  (exactly hits limit)          → success: " + r5 + " | Balance: ₹" + current.getBalance());

        // FixedDepositAccount
        System.out.println("\n-- FixedDepositAccount (locked) --");
        System.out.println("Balance: ₹" + fixedDeposit.getBalance());
        boolean r6 = fixedDeposit.withdraw(1);
        System.out.println("Withdraw ₹1    (any amount rejected) → success: " + r6 + " | Balance: ₹" + fixedDeposit.getBalance());
        boolean r7 = fixedDeposit.withdraw(5000);
        System.out.println("Withdraw ₹5000 (any amount rejected) → success: " + r7 + " | Balance: ₹" + fixedDeposit.getBalance());

        // Zero / negative rejection (applies to all account types)
        System.out.println("\n-- Zero / negative withdrawal guard --");
        System.out.println("Withdraw ₹0   from savings → success: " + savings.withdraw(0));
        System.out.println("Withdraw ₹-50 from savings → success: " + savings.withdraw(-50));

        // ----------------------------------------------------------------
        // PRACTICALS 1–4 DEMONSTRATIONS (preserved)
        // ----------------------------------------------------------------

        // Insufficient-funds example from Practical 1 — now uses SavingsAccount
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

        // toString(), equals(), hashCode()
        System.out.println("\n===== ACCOUNT DETAILS (toString()) =====");
        for (Account account : accounts) {
            System.out.println(account.toString());
        }

        System.out.println("\n===== ACCOUNT EQUALITY (equals() & hashCode()) =====");
        System.out.println("Comparing savings with current : " + savings.equals(current));
        System.out.println("Comparing savings with itself  : " + savings.equals(savings));
        System.out.println("savings hashCode  : " + savings.hashCode());
        System.out.println("current hashCode  : " + current.hashCode());

        // instanceof (non-pattern) from Practical 1 — preserved
        System.out.println("\n===== TYPE CHECKING (instanceof) =====");
        Object obj = savings;
        System.out.println("obj instanceof Account        : " + (obj instanceof Account));
        System.out.println("savings instanceof Object     : " + (savings instanceof Object));
        System.out.println("savings instanceof SavingsAccount : " + (savings instanceof SavingsAccount));

        // Customer, Address & cloning
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

        // Validator
        System.out.println("\n===== INPUT VALIDATION (Validator) =====");
        System.out.println("Valid Mobile (9876543210):         " + Validator.isValidMobile("9876543210"));
        System.out.println("Invalid Mobile (12345):            " + Validator.isValidMobile("12345"));
        System.out.println("Valid Email (rinkesh@example.com): " + Validator.isValidEmail("rinkesh@example.com"));
        System.out.println("Invalid Email (rinkesh@):          " + Validator.isValidEmail("rinkesh@"));
        System.out.println("Valid PAN (ABCDE1234F):            " + Validator.isValidPan("ABCDE1234F"));
        System.out.println("Invalid PAN (ABC123):              " + Validator.isValidPan("ABC123"));
        System.out.println("Valid IFSC (SBIN0001234):          " + Validator.isValidIfsc("SBIN0001234"));
        System.out.println("Invalid IFSC (SBIN123):            " + Validator.isValidIfsc("SBIN123"));

        // CommandParser
        System.out.println("\n===== COMMAND PARSING (CommandParser) =====");
        Command command = CommandParser.parse("DEPOSIT AC0001 500");
        System.out.println("Transaction Type: " + command.type());
        System.out.println("Account Number:   " + command.accountNumber());
        System.out.println("Amount:           " + command.amount());

        // StatementFormatter — works with any Account subclass
        System.out.println("\n===== ACCOUNT STATEMENT (StatementFormatter) =====");
        String statement = StatementFormatter.buildStatement(savings);
        System.out.println(statement);
    }
}