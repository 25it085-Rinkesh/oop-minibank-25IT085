package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Summary outcome of an asynchronous batch transaction processing execution.
 */
public class BatchResult {

    private final int          total;
    private final int          succeeded;
    private final int          failed;
    private final List<String> errors;

    public BatchResult(int total, int succeeded, int failed, List<String> errors) {
        this.total     = total;
        this.succeeded = succeeded;
        this.failed    = failed;
        this.errors    = errors != null ? Collections.unmodifiableList(new ArrayList<>(errors)) : List.of();
    }

    public int getTotal() {
        return total;
    }

    public int getSucceeded() {
        return succeeded;
    }

    public int getFailed() {
        return failed;
    }

    public List<String> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        return String.format("BatchResult[total=%d, succeeded=%d, failed=%d, errors=%d]",
                total, succeeded, failed, errors.size());
    }
}
