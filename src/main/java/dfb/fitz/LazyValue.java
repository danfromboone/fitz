package dfb.fitz;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Thread-safe lazy initialization wrapper implementing {@link Supplier}.
 *
 * @param <T> the type of value supplied
 */
public class LazyValue<T> implements Supplier<T> {
    private volatile Supplier<? extends T> supplier;
    private volatile boolean initialized;
    private T value;

    /**
     * Creates a new {@code LazyValue} that will compute its value using the given supplier on first access.
     *
     * @param supplier the supplier used to compute the value lazily (must not be {@code null})
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public LazyValue(Supplier<? extends T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    /**
     * Factory method that creates a new {@code LazyValue} wrapping the given supplier.
     *
     * @param <T>      the type of value supplied
     * @param supplier the supplier used to compute the value lazily (must not be {@code null})
     * @return a new {@code LazyValue} instance
     * @throws NullPointerException if {@code supplier} is {@code null}
     */
    public static <T> LazyValue<T> of(Supplier<? extends T> supplier) {
        return new LazyValue<>(supplier);
    }

    /**
     * Returns the lazily initialized value, computing and caching it on first invocation.
     *
     * @return the computed value
     */
    @Override
    public T get() {
        if (!initialized) {
            synchronized (this) {
                if (!initialized) {
                    value = supplier.get();
                    initialized = true;
                    supplier = null;
                }
            }
        }
        return value;
    }

    /**
     * Checks whether the value has already been initialized.
     *
     * @return {@code true} if the supplier has completed and the value is cached; {@code false} otherwise
     */
    public boolean isInitialized() {
        return initialized;
    }
}
