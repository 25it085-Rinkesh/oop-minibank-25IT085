package util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/**
 * Appends structured transaction records to a UTF-8 log file.
 *
 * <p>Log format (one record per line):
 * <pre>
 *   {ISO-8601-timestamp}|{account-number}|{DEPOSIT|WITHDRAW}|{amount}
 * </pre>
 * Example:
 * <pre>
 *   2026-10-30T10:15:30Z|AC0001|DEPOSIT|500
 * </pre>
 */
public final class TransactionLog {

    private TransactionLog() {}

    /**
     * Appends one transaction record to {@code file}.
     *
     * <p>The file is created if it does not exist and is never truncated.
     * Parent directories are created automatically.
     *
     * @param file non-null log file path
     * @param line non-null, non-empty record to append; must not contain
     *             newline characters (they would inject extra blank lines)
     * @throws IllegalArgumentException if {@code file} or {@code line} is null,
     *                                  {@code line} is blank, or {@code line}
     *                                  contains a newline character
     * @throws IOException              if writing fails
     */
    public static void append(Path file, String line) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("log file path must not be null");
        }
        if (line == null || line.isBlank()) {
            throw new IllegalArgumentException("log entry must not be null or blank");
        }
        if (line.contains("\n") || line.contains("\r")) {
            throw new IllegalArgumentException(
                    "log entry must not contain newline characters");
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(
                file,
                List.of(line),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }
}

