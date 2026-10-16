package util;

import model.Command;
import model.TransactionType;

public class CommandParser {

    public static Command parse(String line) {
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("Command line cannot be null or blank");
        }

        String[] tokens = line.trim().split("\\s+");
        if (tokens.length != 3) {
            throw new IllegalArgumentException("Invalid command format. Expected exactly 3 tokens: <TYPE> <ACCOUNT_NUMBER> <AMOUNT>");
        }

        TransactionType type;
        try {
            type = TransactionType.valueOf(tokens[0].toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown transaction type: " + tokens[0], e);
        }

        String accountNumber = tokens[1];
        if (accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number cannot be blank");
        }

        long amount;
        try {
            amount = Long.parseLong(tokens[2]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount: " + tokens[2], e);
        }

        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive: " + amount);
        }

        return new Command(type, accountNumber, amount);
    }
}
