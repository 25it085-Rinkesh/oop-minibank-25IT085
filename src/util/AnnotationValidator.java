package util;

import model.annotation.MaxLength;
import model.annotation.Positive;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Inspects an object through Java reflection and returns validation errors
 * produced by the {@code @Positive} and {@code @MaxLength} annotations.
 * The validator traverses the full superclass hierarchy so that annotations
 * declared in {@code Account} are visible when a concrete subclass instance
 * (e.g., {@code SavingsAccount}) is passed.
 */
public class AnnotationValidator {

    /**
     * Validates all annotated fields of {@code obj}, including fields
     * inherited from every superclass up to (but not including) Object.
     *
     * @param obj the object to validate; passing {@code null} returns a
     *            single-element error array documenting the null input.
     * @return an empty array when the object is valid, or one String per
     *         validation error found.
     */
    public static String[] validate(Object obj) {
        if (obj == null) {
            return new String[]{"validation failed: object is null"};
        }

        List<String> errors = new ArrayList<>();

        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                validateField(obj, field, errors);
            }
            clazz = clazz.getSuperclass();
        }

        return errors.toArray(new String[0]);
    }

    private static void validateField(Object obj, Field field, List<String> errors) {
        validatePositive(obj, field, errors);
        validateMaxLength(obj, field, errors);
    }

    private static void validatePositive(Object obj, Field field, List<String> errors) {
        if (!field.isAnnotationPresent(Positive.class)) {
            return;
        }
        Positive annotation = field.getAnnotation(Positive.class);
        Object value = readField(field, obj);

        if (value instanceof Number num) {
            if (num.longValue() <= 0) {
                errors.add(field.getName() + " " + annotation.message());
            }
        } else {
            // @Positive on a non-numeric field is a configuration error.
            errors.add(field.getName() + ": @Positive applied to non-numeric field (type: "
                    + field.getType().getSimpleName() + ")");
        }
    }

    private static void validateMaxLength(Object obj, Field field, List<String> errors) {
        if (!field.isAnnotationPresent(MaxLength.class)) {
            return;
        }
        MaxLength annotation = field.getAnnotation(MaxLength.class);
        Object value = readField(field, obj);

        if (value == null) {
            // Null string is not treated as a length violation.
            return;
        }
        if (value instanceof String str) {
            if (str.length() > annotation.value()) {
                errors.add(field.getName() + " must have at most " + annotation.value() + " characters");
            }
        }
    }

    /**
     * Reads a field value with reflection.
     *
     * @throws IllegalStateException if the field cannot be accessed.
     */
    private static Object readField(Field field, Object obj) {
        try {
            return field.get(obj);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                    "Cannot read field '" + field.getName() + "' on " + obj.getClass().getName(), e);
        }
    }
}
