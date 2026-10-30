package util;

import model.Account;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Saves and loads an Account array using Java object serialization.
 */
public final class StatePersister {

    private StatePersister() {}

    /**
     * Serializes {@code accounts} to {@code file}, creating any missing
     * parent directories automatically.
     *
     * @param accounts non-null array to persist
     * @param file     destination path; must not be null
     * @throws IllegalArgumentException if either argument is null
     * @throws IOException              if writing fails
     */
    public static void save(Account[] accounts, Path file) throws IOException {
        if (accounts == null) {
            throw new IllegalArgumentException("accounts array must not be null");
        }
        if (file == null) {
            throw new IllegalArgumentException("file path must not be null");
        }
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (ObjectOutputStream oos =
                     new ObjectOutputStream(Files.newOutputStream(file))) {
            oos.writeObject(accounts);
        }
    }

    /**
     * Deserializes an Account array from {@code file}.
     *
     * @param file source path; must not be null
     * @return the restored Account array
     * @throws IllegalArgumentException if file is null
     * @throws IOException              if reading fails or the file is corrupt
     * @throws ClassNotFoundException   if a serialized class cannot be resolved
     */
    public static Account[] load(Path file)
            throws IOException, ClassNotFoundException {
        if (file == null) {
            throw new IllegalArgumentException("file path must not be null");
        }
        if (!Files.exists(file)) {
            throw new IOException("Persistence file not found: " + file);
        }
        try (ObjectInputStream ois =
                     new ObjectInputStream(Files.newInputStream(file))) {
            Object obj = ois.readObject();
            if (!(obj instanceof Account[])) {
                throw new IOException(
                        "Unexpected serialized type: " + obj.getClass().getName()
                                + " — expected Account[]");
            }
            return (Account[]) obj;
        }
    }
}

