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
                case DEPOSIT -> "Deposit - To be implemented in a later lab.";
                case WITHDRAW -> "Withdraw - To be implemented in a later lab.";
                case TRANSFER -> "Transfer - To be implemented in a later lab.";
                case EXIT -> "Thank you for using MiniBank. Goodbye!";
            };

            System.out.println(message);

        } while (choice != 5);

        sc.close();

        Account[] accounts = {
                new Account("Rinkesh", 1000),
                new Account("Customer2"),
                new Account("Customer3", 5000)
        };

        accounts[0].deposit(500);
        accounts[0].withdraw(200);

        accounts[1].deposit(2000);
        accounts[1].withdraw(750);

        accounts[2].deposit(1000);
        accounts[2].withdraw(2500);

        long overdraftAmount = 5000;
        System.out.println("\nAttempting to withdraw ₹" + overdraftAmount + " from " + accounts[0].getOwnerName() + "'s account (Current Balance: ₹" + accounts[0].getBalance() + ")...");
        boolean withdrawSuccess = accounts[0].withdraw(overdraftAmount);
        System.out.println("Withdrawal successful: " + withdrawSuccess);
        if (!withdrawSuccess) {
            System.out.println("Transaction rejected: Insufficient funds. Balance remains: ₹" + accounts[0].getBalance());
        }

        System.out.println("\n===== ACCOUNT DETAILS (toString()) =====");
        for (Account account : accounts) {
            System.out.println(account.toString());
        }

        System.out.println("\n===== ACCOUNT EQUALITY (equals() & hashCode()) =====");
        System.out.println("Comparing accounts[0] with accounts[1]: " + accounts[0].equals(accounts[1]));
        System.out.println("Comparing accounts[0] with itself:        " + accounts[0].equals(accounts[0]));
        System.out.println("accounts[0] hashCode: " + accounts[0].hashCode());
        System.out.println("accounts[1] hashCode: " + accounts[1].hashCode());

        System.out.println("\n===== TYPE CHECKING (instanceof) =====");
        Object obj = accounts[0];
        System.out.println("obj (referencing Account) instanceof Account:  " + (obj instanceof Account));
        System.out.println("accounts[0] instanceof Object:                 " + (accounts[0] instanceof Object));

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

        System.out.println("\n===== INPUT VALIDATION (Validator) =====");
        System.out.println("Valid Mobile (9876543210):         " + Validator.isValidMobile("9876543210"));
        System.out.println("Invalid Mobile (12345):            " + Validator.isValidMobile("12345"));
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
        String statement = StatementFormatter.buildStatement(accounts[0]);
        System.out.println(statement);
    }
}