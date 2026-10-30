package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Summary outcome of an asynchronous batch transaction processing execution.
 *
 * <p>Tracks requested, accepted, rejected, succeeded, failed, and completed counts
 * ensuring:
 * <ul>
 *   <li>{@code totalRequested = accepted + rejected}</li>
 *   <li>{@code completed = succeeded + failed}</li>
 *   <li>{@code accepted = completed}</li>
 * </ul>
 */
public class BatchResult {

    private final int          totalRequested;
    private final int          accepted;
    private final int          succeeded;
    private final int          failed;
    private final int          rejected;
    private final int          completed;
    private final List<String> errors;

    public BatchResult(int totalRequested, int accepted, int succeeded, int failed, int rejected, List<String> errors) {
        this.totalRequested = totalRequested;
        this.accepted       = accepted;
        this.succeeded      = succeeded;
        this.failed         = failed;
        this.rejected       = rejected;
        this.completed      = succeeded + failed;
        this.errors         = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
    }

    public BatchResult(int total, int succeeded, int failed, List<String> errors) {
        this(total, total, succeeded, failed, 0, errors);
    }

    public int getTotalRequested() {
        return totalRequested;
    }

    public int getTotal() {
        return totalRequested;
    }

    public int getAccepted() {
        return accepted;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public int getRejected() {
        return rejected;
    }

    public int getCompleted() {
        return completed;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return String.format(
                "BatchResult[totalRequested=%d, accepted=%d, succeeded=%d, failed=%d, rejected=%d, completed=%d, errors=%d]",
                totalRequested, accepted, succeeded, failed, rejected, completed, errors.size());
    }
}
