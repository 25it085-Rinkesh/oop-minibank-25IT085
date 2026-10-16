package util;

import model.Account;

public class StatementFormatter {

    public static String buildStatement(Account account) {
        if (account == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }

        StringBuilder sb = new StringBuilder();
        sb.append("========== ACCOUNT STATEMENT ==========\n");
        sb.append("Account Number : ").append(account.getAccountNumber()).append("\n");
        sb.append("Owner Name     : ").append(account.getOwnerName()).append("\n");
        sb.append("Balance        : ₹").append(account.getBalance()).append("\n");
        sb.append("Account Status : ").append(account.isActive() ? "Active" : "Inactive").append("\n");
        sb.append("=======================================");

        return sb.toString();
    }
}
