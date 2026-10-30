package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Reads all {@code *.log} files in a directory and generates a UTF-8 summary
 * report using Java NIO.
 *
 * <p>Expected log-line format (from {@link TransactionLog}):
 * <pre>
 *   {ISO-8601-timestamp}|{account-number}|{DEPOSIT|WITHDRAW}|{amount}
 * </pre>
 */
public final class ReportGenerator {

    private static final DateTimeFormatter TS_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneOffset.UTC);

    private ReportGenerator() {}

    /**
     * Walks {@code logsDirectory}, aggregates transaction totals across every
     * {@code *.log} file, and writes the summary to {@code reportFile}.
     *
     * @param logsDirectory directory containing {@code *.log} files
     * @param reportFile    destination report path (created or overwritten)
     * @throws IOException if directory walking, file reading, or writing fails
     */
    public static void generate(Path logsDirectory, Path reportFile)
            throws IOException {
        if (logsDirectory == null || reportFile == null) {
            throw new IllegalArgumentException("logsDirectory and reportFile must not be null");
        }
        Files.createDirectories(logsDirectory);

        Path reportFileName = reportFile.getFileName();

        long totalDeposits    = 0L;
        long totalWithdrawals = 0L;
        long invalidEntries   = 0L;
        int  filesProcessed   = 0;

        StringBuilder fileDetails = new StringBuilder();

        try (DirectoryStream<Path> stream =
                     Files.newDirectoryStream(logsDirectory, "*.log")) {

            for (Path logPath : stream) {
                // Never count the report file if it happens to end with .log
                if (logPath.getFileName().equals(reportFileName)) {
                    continue;
                }
                if (!Files.isRegularFile(logPath)) {
                    continue;
                }

                BasicFileAttributes attrs =
                        Files.readAttributes(logPath, BasicFileAttributes.class);
                String lastModified = TS_FMT.format(attrs.lastModifiedTime().toInstant());

                filesProcessed++;
                fileDetails.append("  File Name       : ")
                           .append(logPath.getFileName()).append("\n");
                fileDetails.append("  Size            : ")
                           .append(attrs.size()).append(" bytes\n");
                fileDetails.append("  Last Modified   : ")
                           .append(lastModified).append("\n\n");

                try (BufferedReader reader =
                             Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        String[] parts = line.split("\\|", -1);
                        if (parts.length != 4) {
                            invalidEntries++;
                            continue;
                        }
                        String type   = parts[2].trim();
                        String amtStr = parts[3].trim();
                        long amount;
                        try {
                            amount = Long.parseLong(amtStr);
                        } catch (NumberFormatException e) {
                            invalidEntries++;
                            continue;
                        }
                        if (amount <= 0) {
                            invalidEntries++;
                            continue;
                        }
                        switch (type) {
                            case "DEPOSIT"  -> totalDeposits    = Math.addExact(totalDeposits,    amount);
                            case "WITHDRAW" -> totalWithdrawals = Math.addExact(totalWithdrawals, amount);
                            default         -> invalidEntries++;
                        }
                    }
                }
            }
        }

        // Net movement: safe subtraction (both totals are non-negative).
        long netMovement = totalDeposits - totalWithdrawals;

        StringBuilder report = new StringBuilder();
        report.append("MINIBANK — END OF DAY REPORT\n");
        report.append("========================================\n\n");
        report.append(String.format("Log Files Processed : %d%n",   filesProcessed));
        report.append(String.format("Total Deposits      : \u20b9%d%n", totalDeposits));
        report.append(String.format("Total Withdrawals   : \u20b9%d%n", totalWithdrawals));
        report.append(String.format("Net Movement        : \u20b9%d%n", netMovement));
        report.append(String.format("Invalid Entries     : %d%n",   invalidEntries));

        if (filesProcessed > 0) {
            report.append("\nLOG FILE DETAILS\n");
            report.append("----------------------------------------\n");
            report.append(fileDetails);
        }

        report.append("========================================\n");

        Path reportParent = reportFile.getParent();
        if (reportParent != null) {
            Files.createDirectories(reportParent);
        }
        Files.writeString(reportFile, report.toString(), StandardCharsets.UTF_8);
    }
}

