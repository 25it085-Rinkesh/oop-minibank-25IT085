package repository;

import model.annotation.Id;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Generic in-memory repository backed by a {@link HashMap}.
 *
 * <p>Entities are identified by the single field annotated with {@link Id}.
 * Field discovery traverses the class hierarchy so inherited ID fields are supported.
 *
 * @param <T> the entity type
 */
public class Repository<T> {

    private final HashMap<String, T> storage = new HashMap<>();

    /**
     * Saves an entity in the repository.
     *
     * <p>Extracts the ID via reflection from the field annotated with {@link Id}.
     * If an entity with the same ID already exists, it is updated/replaced.
     *
     * @param entity the entity to save; must not be null
     * @return the saved entity
     * @throws IllegalArgumentException if entity is null, no {@code @Id} field is found,
     *                                  multiple {@code @Id} fields exist, or the ID value is null/blank
     * @throws IllegalStateException    if the {@code @Id} field cannot be accessed reflectively
     */
    public T save(T entity) {
        String id = extractId(entity);
        storage.put(id, entity);
        return entity;
    }

    /**
     * Retrieves an entity by its string ID.
     *
     * @param id the identifier; must not be null or blank
     * @return the matching entity, or {@code null} if not found
     * @throws IllegalArgumentException if id is null or blank
     */
    public T findById(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        return storage.get(id);
    }

    /**
     * Returns a defensive snapshot of all stored entities.
     *
     * @return a new {@link ArrayList} containing all entities in arbitrary order
     */
    public ArrayList<T> findAll() {
        return new ArrayList<>(storage.values());
    }

    /**
     * Deletes an entity by its ID.
     *
     * @param id the identifier; must not be null or blank
     * @return {@code true} if an entity was removed, {@code false} if not found
     * @throws IllegalArgumentException if id is null or blank
     */
    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("id must not be null or blank");
        }
        return storage.remove(id) != null;
    }

    /**
     * Returns the number of entities currently stored.
     *
     * @return current repository size
     */
    public int size() {
        return storage.size();
    }

    /**
     * Saves multiple entities to the repository.
     *
     * @param items the entities to save; array and elements must not be null
     * @throws IllegalArgumentException if items is null or contains invalid entities
     */
    @SafeVarargs
    public final void saveAll(T... items) {
        if (items == null) {
            throw new IllegalArgumentException("items array must not be null");
        }
        for (T item : items) {
            save(item);
        }
    }

    /**
     * Calculates the arithmetic mean of an array of numbers.
     *
     * @param <N>    numeric type extending {@link Number}
     * @param values array of numbers; must not be null, empty, or contain null elements
     * @return the arithmetic mean as a double
     * @throws IllegalArgumentException if values is null, empty, or contains null elements
     */
    public static <N extends Number> double average(N[] values) {
        if (values == null) {
            throw new IllegalArgumentException("values array must not be null");
        }
        if (values.length == 0) {
            throw new IllegalArgumentException("values array must not be empty");
        }
        double sum = 0.0;
        for (N val : values) {
            if (val == null) {
                throw new IllegalArgumentException("values array element must not be null");
            }
            sum += val.doubleValue();
        }
        return sum / values.length;
    }

    /**
     * Discovers and extracts the string ID from the given entity using reflection.
     */
    private String extractId(T entity) {
        if (entity == null) {
            throw new IllegalArgumentException("entity must not be null");
        }

        Field idField = null;
        Class<?> current = entity.getClass();

        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    if (idField != null) {
                        throw new IllegalArgumentException(
                                "Multiple @Id annotations found in hierarchy of "
                                        + entity.getClass().getName());
                    }
                    idField = field;
                }
            }
            current = current.getSuperclass();
        }

        if (idField == null) {
            throw new IllegalArgumentException(
                    "No @Id field found on entity class: " + entity.getClass().getName());
        }

        try {
            idField.setAccessible(true);
            Object value = idField.get(entity);
            if (value == null) {
                throw new IllegalArgumentException(
                        "@Id field '" + idField.getName() + "' value cannot be null");
            }
            String idStr = value.toString();
            if (idStr.isBlank()) {
                throw new IllegalArgumentException(
                        "@Id field '" + idField.getName() + "' value cannot be blank");
            }
            return idStr;
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Failed to access @Id field '" + idField.getName() + "': " + e.getMessage(), e);
        }
    }
}
