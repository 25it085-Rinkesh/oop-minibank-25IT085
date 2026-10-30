package service;

import exception.AccountNotFoundException;
import exception.BankException;
import exception.InvalidAmountException;
import model.Account;
import model.CurrentAccount;
import model.FixedDepositAccount;
import model.SavingsAccount;
import model.TransactionType;
import model.annotation.Audit;
import repository.Repository;
import util.AnnotationValidator;
import util.StatePersister;
import util.StatementFormatter;
import util.TransactionLog;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Core banking service integrating repository persistence, transaction processing,
 * validation, transaction logging, and operational auditing.
 */
public class BankService {

    private final Repository<Account>  repository;
    private final TransactionProcessor processor;
    private final Path                 logsDir;
    private final Path                 auditLogFile;
    private final Path                 sequenceFile;

    public BankService(Repository<Account> repository, TransactionProcessor processor, Path logsDir, Path sequenceFile) {
        this.repository   = Objects.requireNonNull(repository, "repository must not be null");
        this.processor    = Objects.requireNonNull(processor, "processor must not be null");
        this.logsDir      = logsDir != null ? logsDir : Paths.get("data", "logs");
        this.auditLogFile = Paths.get("data", "audit", "audit.log");
        this.sequenceFile = sequenceFile != null ? sequenceFile : Paths.get("data", "sequence.dat");
    }

    public BankService(Repository<Account> repository, TransactionProcessor processor, Path logsDir) {
        this(repository, processor, logsDir, Paths.get("data", "sequence.dat"));
    }

    public BankService(Repository<Account> repository, TransactionProcessor processor) {
        this(repository, processor, Paths.get("data", "logs"), Paths.get("data", "sequence.dat"));
    }

    // -------------------------------------------------------------------------
    // 2.1 openAccount
    // -------------------------------------------------------------------------

    @Audit("OPEN_ACCOUNT")
    public SavingsAccount openSavingsAccount(String ownerName, long openingBalance, long minBalance)
            throws BankException {
        validateOwner(ownerName);
        if (openingBalance <= 0) {
            throw new InvalidAmountException("Opening balance must be positive, but was: " + openingBalance);
        }
        if (minBalance < 0) {
            throw new InvalidAmountException("Minimum balance cannot be negative: " + minBalance);
        }

        SavingsAccount account = new SavingsAccount(ownerName.trim(), openingBalance, minBalance);
        validateAccountModel(account);
        saveAndRegister(account);
        logTransaction(account.getAccountNumber(), TransactionType.DEPOSIT, openingBalance);
        recordAudit("openSavingsAccount", account.getAccountNumber(), "balance=" + openingBalance + ", min=" + minBalance);
        return account;
    }

    @Audit("OPEN_ACCOUNT")
    public CurrentAccount openCurrentAccount(String ownerName, long openingBalance, long overdraftLimit)
            throws BankException {
        validateOwner(ownerName);
        if (openingBalance <= 0) {
            throw new InvalidAmountException("Opening balance must be positive, but was: " + openingBalance);
        }
        if (overdraftLimit < 0) {
            throw new InvalidAmountException("Overdraft limit cannot be negative: " + overdraftLimit);
        }

        CurrentAccount account = new CurrentAccount(ownerName.trim(), openingBalance, overdraftLimit);
        validateAccountModel(account);
        saveAndRegister(account);
        logTransaction(account.getAccountNumber(), TransactionType.DEPOSIT, openingBalance);
        recordAudit("openCurrentAccount", account.getAccountNumber(), "balance=" + openingBalance + ", overdraft=" + overdraftLimit);
        return account;
    }

    @Audit("OPEN_ACCOUNT")
    public FixedDepositAccount openFixedDepositAccount(String ownerName, long openingBalance)
            throws BankException {
        validateOwner(ownerName);
        if (openingBalance <= 0) {
            throw new InvalidAmountException("Opening balance must be positive, but was: " + openingBalance);
        }

        FixedDepositAccount account = new FixedDepositAccount(ownerName.trim(), openingBalance);
        validateAccountModel(account);
        saveAndRegister(account);
        logTransaction(account.getAccountNumber(), TransactionType.DEPOSIT, openingBalance);
        recordAudit("openFixedDepositAccount", account.getAccountNumber(), "balance=" + openingBalance);
        return account;
    }

    public Account openAccount(String accountType, String ownerName, long openingBalance, long extraParam)
            throws BankException {
        if (accountType == null || accountType.isBlank()) {
            throw new InvalidAmountException("Account type cannot be null or blank");
        }
        String type = accountType.trim().toUpperCase();
        return switch (type) {
            case "SAVINGS", "SAVING", "1" -> openSavingsAccount(ownerName, openingBalance, extraParam);
            case "CURRENT", "2"           -> openCurrentAccount(ownerName, openingBalance, extraParam);
            case "FIXED_DEPOSIT", "FD", "3" -> openFixedDepositAccount(ownerName, openingBalance);
            default -> throw new InvalidAmountException("Unsupported account type: " + accountType
                    + " (supported: SAVINGS, CURRENT, FIXED_DEPOSIT)");
        };
    }

    // -------------------------------------------------------------------------
    // 2.2 deposit
    // -------------------------------------------------------------------------

    @Audit("DEPOSIT")
    public void deposit(String accountNumber, long amount) throws BankException {
        Account account = findAccount(accountNumber);
        account.deposit(amount);
        logTransaction(account.getAccountNumber(), TransactionType.DEPOSIT, amount);
        recordAudit("deposit", accountNumber, "amount=" + amount);
    }

    // -------------------------------------------------------------------------
    // 2.3 withdraw
    // -------------------------------------------------------------------------

    @Audit("WITHDRAW")
    public boolean withdraw(String accountNumber, long amount) throws BankException {
        Account account = findAccount(accountNumber);
        boolean result = account.withdraw(amount);
        if (result) {
            logTransaction(account.getAccountNumber(), TransactionType.WITHDRAW, amount);
            recordAudit("withdraw", accountNumber, "amount=" + amount);
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // 2.4 transfer
    // -------------------------------------------------------------------------

    @Audit("TRANSFER")
    public void transfer(String fromAccountNumber, String toAccountNumber, long amount)
            throws BankException {
        if (fromAccountNumber == null || fromAccountNumber.isBlank()) {
            throw new AccountNotFoundException("Source account number cannot be null or blank.");
        }
        if (toAccountNumber == null || toAccountNumber.isBlank()) {
            throw new AccountNotFoundException("Transfer failed: destination account is null.");
        }

        Account from = repository.findById(fromAccountNumber.trim());
        if (from == null) {
            throw new AccountNotFoundException("Source account not found: " + fromAccountNumber);
        }

        Account to = repository.findById(toAccountNumber.trim());
        if (to == null) {
            throw new AccountNotFoundException("Destination account not found: " + toAccountNumber);
        }

        // Safe transfer with ordered locking and automatic rollback
        from.transfer(to, amount);

        // Record debit on source and credit on destination for audit & statement tracking
        logTransaction(from.getAccountNumber(), TransactionType.WITHDRAW, amount);
        logTransaction(to.getAccountNumber(), TransactionType.DEPOSIT, amount);
        recordAudit("transfer", fromAccountNumber + "->" + toAccountNumber, "amount=" + amount);
    }

    // -------------------------------------------------------------------------
    // 2.5 statement
    // -------------------------------------------------------------------------

    public String statement(String accountNumber) throws AccountNotFoundException {
        Account account = findAccount(accountNumber);

        StringBuilder sb = new StringBuilder();
        sb.append(StatementFormatter.buildStatement(account)).append("\n\n");
        sb.append("TRANSACTION HISTORY\n");
        sb.append("------------------------------------------------------------\n");
        sb.append(String.format("%-22s  %-10s  %-12s%n", "Date / Time (UTC)", "Type", "Amount"));
        sb.append("------------------------------------------------------------\n");

        List<String> records = readAccountTransactions(account.getAccountNumber());
        if (records.isEmpty()) {
            sb.append("No transaction history recorded for this account.\n");
        } else {
            for (String rec : records) {
                sb.append(rec).append("\n");
            }
        }
        sb.append("============================================================\n");
        return sb.toString();
    }

    // -------------------------------------------------------------------------
    // 2.6 listAccounts & lookup
    // -------------------------------------------------------------------------

    public ArrayList<Account> listAccounts() {
        return repository.findAll();
    }

    public Account getAccount(String accountNumber) throws AccountNotFoundException {
        return findAccount(accountNumber);
    }

    public int getAccountCount() {
        return repository.size();
    }

    // -------------------------------------------------------------------------
    // Batch processing
    // -------------------------------------------------------------------------

    /**
     * Executes a batch of transaction instructions concurrently using the
     * shared TransactionProcessor. Waits for all tasks to complete before returning.
     */
    public BatchResult processBatch(List<BatchTransaction> batch) {
        if (batch == null || batch.isEmpty()) {
            return new BatchResult(0, 0, 0, List.of());
        }

        CountDownLatch latch = new CountDownLatch(batch.size());
        AtomicInteger succeeded = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        List<String> errors = Collections.synchronizedList(new ArrayList<>());

        for (BatchTransaction tx : batch) {
            processor.submit(() -> {
                try {
                    switch (tx.getType()) {
                        case DEPOSIT  -> deposit(tx.getAccountNumber(), tx.getAmount());
                        case WITHDRAW -> withdraw(tx.getAccountNumber(), tx.getAmount());
                        default       -> throw new UnsupportedOperationException(
                                "Unsupported batch transaction type: " + tx.getType());
                    }
                    succeeded.incrementAndGet();
                } catch (Exception e) {
                    failed.incrementAndGet();
                    errors.add(String.format("[%s %s ₹%d]: %s",
                            tx.getAccountNumber(), tx.getType(), tx.getAmount(), e.getMessage()));
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            boolean finished = latch.await(60, TimeUnit.SECONDS);
            if (!finished) {
                errors.add("Batch processing timed out after 60 seconds.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            errors.add("Batch processing thread was interrupted.");
        }

        return new BatchResult(batch.size(), succeeded.get(), failed.get(), errors);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Account findAccount(String accountNumber) throws AccountNotFoundException {
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new AccountNotFoundException("Account number cannot be null or blank.");
        }
        Account account = repository.findById(accountNumber.trim());
        if (account == null) {
            throw new AccountNotFoundException("Account not found: " + accountNumber.trim());
        }
        return account;
    }

    private void saveAndRegister(Account account) {
        repository.save(account);
        try {
            processor.registerAccount(account);
        } catch (IllegalStateException ignored) {
            // Account already registered in processor
        }
        persistSequence();
    }

    private void persistSequence() {
        if (sequenceFile != null) {
            try {
                Path parent = sequenceFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(sequenceFile, String.valueOf(Account.getAccountCounter()), StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }
    }

    /**
     * Recovers the highest allocated account number by scanning persisted
     * accounts, all historical transaction logs in logsDir, and any durable sequence
     * tracking file. Synchronizes {@link Account#syncAccountCounter(long)} to at least
     * this maximum and records it to sequenceFile.
     *
     * @param accountsFile path to persisted accounts file (e.g. data/accounts.dat)
     * @param logsDir      path to logs directory (e.g. data/logs)
     * @param sequenceFile path to durable sequence tracking file (e.g. data/sequence.dat)
     * @return the highest recovered numeric account ID
     */
    public static synchronized long recoverAndSyncAccountSequence(Path accountsFile, Path logsDir, Path sequenceFile) {
        long maxId = 0L;

        // 1. Scan durable sequence file if present
        if (sequenceFile != null && Files.exists(sequenceFile)) {
            try {
                String content = Files.readString(sequenceFile, StandardCharsets.UTF_8).trim();
                if (!content.isEmpty()) {
                    long seq = Long.parseLong(content);
                    if (seq > maxId) {
                        maxId = seq;
                    }
                }
            } catch (Exception ignored) {
                // Ignore corrupt sequence file content
            }
        }

        // 2. Scan persisted accounts file if present
        if (accountsFile != null && Files.exists(accountsFile)) {
            try {
                Account[] accounts = StatePersister.load(accountsFile);
                if (accounts != null) {
                    for (Account a : accounts) {
                        if (a != null && a.getAccountNumber() != null) {
                            long num = extractAccountNumericId(a.getAccountNumber());
                            if (num > maxId) {
                                maxId = num;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
                // If accounts.dat cannot be deserialized, continue to logs
            }
        }

        // 3. Scan all historical transaction logs in logsDir (*.log)
        if (logsDir != null && Files.exists(logsDir) && Files.isDirectory(logsDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, "*.log")) {
                for (Path logPath : stream) {
                    if (!Files.isRegularFile(logPath) || logPath.getFileName().toString().equals("audit.log")) {
                        continue;
                    }
                    try (BufferedReader reader = Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.isBlank()) {
                                continue;
                            }
                            String[] parts = line.split("\\|", -1);
                            if (parts.length >= 2) {
                                long num = extractAccountNumericId(parts[1].trim());
                                if (num > maxId) {
                                    maxId = num;
                                }
                            }
                        }
                    } catch (IOException ignored) {
                        // Skip unreadable log file
                    }
                }
            } catch (IOException ignored) {
                // Directory enumeration error
            }
        }

        // 4. Synchronize static counter in Account model
        Account.syncAccountCounter(maxId);

        // 5. Update durable sequence file so current maxId is persisted
        if (sequenceFile != null) {
            try {
                Path parent = sequenceFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                Files.writeString(sequenceFile, String.valueOf(Account.getAccountCounter()), StandardCharsets.UTF_8);
            } catch (IOException ignored) {}
        }

        return Account.getAccountCounter();
    }

    /**
     * Recovers and synchronizes the account sequence using default sequence.dat path.
     */
    public static synchronized long recoverAndSyncAccountSequence(Path accountsFile, Path logsDir) {
        Path seqFile = (accountsFile != null && accountsFile.getParent() != null)
                ? accountsFile.getParent().resolve("sequence.dat")
                : Paths.get("data", "sequence.dat");
        return recoverAndSyncAccountSequence(accountsFile, logsDir, seqFile);
    }

    private static long extractAccountNumericId(String accountNumber) {
        if (accountNumber == null || !accountNumber.startsWith("AC")) {
            return 0L;
        }
        try {
            return Long.parseLong(accountNumber.substring(2));
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void validateOwner(String ownerName) throws InvalidAmountException {
        if (ownerName == null || ownerName.isBlank()) {
            throw new InvalidAmountException("Owner name cannot be null or blank.");
        }
        if (ownerName.trim().length() > 50) {
            throw new InvalidAmountException("Owner name must have at most 50 characters.");
        }
    }

    private void validateAccountModel(Account account) throws InvalidAmountException {
        String[] errors = AnnotationValidator.validate(account);
        if (errors.length > 0) {
            throw new InvalidAmountException(String.join("; ", errors));
        }
    }

    private Path getDailyLogFile() {
        String today = LocalDate.now().toString();
        return logsDir.resolve("transactions-" + today + ".log");
    }

    private void logTransaction(String accountNumber, TransactionType type, long amount) {
        try {
            String ts = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
            String entry = ts + "|" + accountNumber + "|" + type + "|" + amount;
            TransactionLog.append(getDailyLogFile(), entry);
        } catch (IOException e) {
            System.err.println("Warning: Failed to append to transaction log: " + e.getMessage());
        }
    }

    private List<String> readAccountTransactions(String accountNumber) {
        List<String> list = new ArrayList<>();
        if (!Files.exists(logsDir)) {
            return list;
        }

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(logsDir, "*.log")) {
            for (Path logPath : stream) {
                if (logPath.getFileName().toString().equals("audit.log")) {
                    continue;
                }
                if (!Files.isRegularFile(logPath)) {
                    continue;
                }
                try (BufferedReader reader = Files.newBufferedReader(logPath, StandardCharsets.UTF_8)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isBlank()) {
                            continue;
                        }
                        String[] parts = line.split("\\|", -1);
                        if (parts.length == 4 && parts[1].trim().equals(accountNumber)) {
                            list.add(String.format("%-22s  %-10s  ₹%-12s",
                                    parts[0].trim(), parts[2].trim(), parts[3].trim()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Warning: Error reading account transaction logs: " + e.getMessage());
        }
        return list;
    }

    /**
     * Reflective operational audit logging for methods annotated with {@link Audit}.
     */
    private void recordAudit(String methodName, String target, String details) {
        try {
            boolean hasAudit = false;
            for (Method m : getClass().getDeclaredMethods()) {
                if (m.getName().equals(methodName) && m.isAnnotationPresent(Audit.class)) {
                    hasAudit = true;
                    break;
                }
            }
            if (hasAudit) {
                String ts = Instant.now().truncatedTo(ChronoUnit.SECONDS).toString();
                String auditEntry = String.format("%s [AUDIT] method=%s target=%s details={%s}",
                        ts, methodName, target, details);
                Path parent = auditLogFile.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                TransactionLog.append(auditLogFile, auditEntry);
            }
        } catch (Exception ignored) {
            // Audit failure must never alter business results
        }
    }
}
