package service;

import model.TransactionType;

/**
 * A bounded, circular buffer that coordinates one producer thread and one
 * consumer thread using Java monitor primitives (wait / notifyAll).
 *
 * <p>Transactions are represented as simple {@code long[]} pairs:
 * {@code [0]} = {@link TransactionType#ordinal()}, {@code [1]} = amount.
 *
 * <p>A sentinel value with amount {@code Long.MIN_VALUE} signals end-of-stream
 * to the consumer.
 */
public class TransactionBuffer {

    public static final long   POISON_AMOUNT = Long.MIN_VALUE;
    private static final int   CAPACITY      = 5;

    private final long[][] buffer = new long[CAPACITY][2];
    private int  head  = 0;   // next slot to read
    private int  tail  = 0;   // next slot to write
    private int  count = 0;   // items currently in the buffer

    /**
     * Inserts a transaction into the buffer. Blocks while the buffer is full.
     * Uses a {@code while} loop to guard against spurious wakeups.
     */
    public synchronized void produce(TransactionType type, long amount)
            throws InterruptedException {
        while (count == CAPACITY) {
            wait();
        }
        buffer[tail][0] = type.ordinal();
        buffer[tail][1] = amount;
        tail = (tail + 1) % CAPACITY;
        count++;
        notifyAll();
    }

    /**
     * Inserts the end-of-stream poison pill into the buffer.
     * Blocks while the buffer is full.
     */
    public synchronized void producePoison() throws InterruptedException {
        produce(TransactionType.DEPOSIT, POISON_AMOUNT);
    }

    /**
     * Retrieves the next transaction from the buffer. Blocks while the buffer is
     * empty. Uses a {@code while} loop to guard against spurious wakeups.
     *
     * @return a two-element array: {@code [typeOrdinal, amount]}
     */
    public synchronized long[] consume() throws InterruptedException {
        while (count == 0) {
            wait();
        }
        long[] item = new long[] { buffer[head][0], buffer[head][1] };
        head = (head + 1) % CAPACITY;
        count--;
        notifyAll();
        return item;
    }
}
