package service;

/**
 * A simple AutoCloseable resource used to demonstrate try-with-resources
 * and deterministic cleanup in MiniBank.
 */
public class DemoResource implements AutoCloseable {

    public DemoResource() {
        System.out.println("[DemoResource] Resource acquired.");
    }

    /** Simulates using the resource for a brief bank-audit operation. */
    public void use() {
        System.out.println("[DemoResource] Performing audit check... OK.");
    }

    @Override
    public void close() {
        System.out.println("[DemoResource] Resource released (close() called automatically).");
    }
}
