package dfb.di;

import java.time.Duration;
import java.util.Objects;

/**
 * Utility methods for validating and normalizing method and constructor arguments.
 */
public final class Validate {

    private Validate() {
    }

    /**
     * Validates that the given reference is not {@code null}.
     *
     * @param <T>   the type of the reference
     * @param value the value to check
     * @param name  the parameter or field name used in the exception message
     * @return {@code value} if not {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <T> T nonNull(T value, String name) {
        return Objects.requireNonNull(value, name + " must not be null");
    }

    /**
     * Validates that the given string is not {@code null} and not blank after trimming,
     * and returns the trimmed string.
     *
     * @param value the string value to validate and trim
     * @param name  the parameter or field name used in exception messages
     * @return the trimmed non-empty string
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if the trimmed string is empty
     */
    public static String trimmedNonBlank(String value, String name) {
        Objects.requireNonNull(value, name + " must not be null");
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return trimmed;
    }

    /**
     * Trims the given string if non-null, or returns an empty string {@code ""} if {@code null}.
     *
     * @param value the string to trim; may be {@code null}
     * @return the trimmed string, or {@code ""} if {@code value} is {@code null}
     */
    public static String trimToEmpty(String value) {
        return (value != null) ? value.trim() : "";
    }

    /**
     * Validates that an integer value falls within the specified inclusive range.
     *
     * @param value        the integer value to check
     * @param minInclusive the minimum allowed value (inclusive)
     * @param maxInclusive the maximum allowed value (inclusive)
     * @param name         the parameter or field name used in the exception message
     * @return {@code value} if within {@code [minInclusive..maxInclusive]}
     * @throws IllegalArgumentException if {@code value < minInclusive} or {@code value > maxInclusive}
     */
    public static int inRange(int value, int minInclusive, int maxInclusive, String name) {
        if (value < minInclusive || value > maxInclusive) {
            throw new IllegalArgumentException(
                    name + " must be between " + minInclusive + " and " + maxInclusive + ": " + value
            );
        }
        return value;
    }

    /**
     * Validates that the given {@link Duration} is non-null and strictly positive.
     *
     * @param duration the duration to check
     * @param name     the parameter or field name used in exception messages
     * @return {@code duration} if non-null and positive
     * @throws NullPointerException     if {@code duration} is {@code null}
     * @throws IllegalArgumentException if {@code duration} is zero or negative
     */
    public static Duration positive(Duration duration, String name) {
        Objects.requireNonNull(duration, name + " must not be null");
        if (duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }
}
