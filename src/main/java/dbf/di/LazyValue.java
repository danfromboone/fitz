package dbf.di;

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

    public LazyValue(Supplier<? extends T> supplier) {
        this.supplier = Objects.requireNonNull(supplier, "supplier must not be null");
    }

    public static <T> LazyValue<T> of(Supplier<? extends T> supplier) {
        return new LazyValue<>(supplier);
    }

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

    public boolean isInitialized() {
        return initialized;
    }
}
