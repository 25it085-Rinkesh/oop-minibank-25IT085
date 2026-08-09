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

        int choice;

        do {
            System.out.println("\n========== MENU ==========");
            System.out.println("1. Open Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");

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

            switch (option) {
                case OPEN_ACCOUNT ->
                        System.out.println("Open Account - To be implemented in a later lab.");

                case DEPOSIT ->
                        System.out.println("Deposit - To be implemented in a later lab.");

                case WITHDRAW ->
                        System.out.println("Withdraw - To be implemented in a later lab.");

                case TRANSFER ->
                        System.out.println("Transfer - To be implemented in a later lab.");

                case EXIT ->
                        System.out.println("Thank you for using MiniBank. Goodbye!");
            }

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

        System.out.println("\n===== ACCOUNT BALANCES =====");

        for (Account account : accounts) {
            System.out.println(
                    account.getAccountNumber()
                            + " | "
                            + account.getOwnerName()
                            + " | Balance: ₹"
                            + account.getBalance()
            );
        }
    }
}