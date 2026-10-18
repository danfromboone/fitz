package dfb.fitz;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;

/**
 * Utility methods for safely closing {@link Closeable} and {@link AutoCloseable} resources
 * during application or component shutdown without propagating exceptions.
 * <p>
 * Any exception thrown while closing a resource is caught and logged as a warning so that
 * shutdown routines remain concise and a failure in one resource does not prevent remaining
 * resources from being closed.
 */
public final class Closeables {
    private static final Logger log = LoggerFactory.getLogger(Closeables.class);

    private Closeables() {
    }

    /**
     * Safely closes the given {@link AutoCloseable} (including {@link Closeable}),
     * catching and logging any exception that occurs.
     * <p>
     * If {@code closeable} is {@code null}, this method is a no-op.
     *
     * @param closeable the resource to close; may be {@code null}
     */
    public static void closeQuietly(AutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (Exception e) {
            log.warn("Failed to close resource: {}", closeable, e);
        }
    }

    /**
     * Safely closes the given object if it implements {@link AutoCloseable} (including {@link Closeable}),
     * or if it is an initialized {@link LazyValue} wrapping a closeable resource.
     * <p>
     * If {@code resource} is {@code null}, does not implement {@link AutoCloseable}, or is an
     * uninitialized {@link LazyValue}, this method is a no-op. Any exception thrown during
     * {@code close()} is caught and logged.
     *
     * @param resource the candidate object to close; may be {@code null}
     */
    public static void closeQuietly(Object resource) {
        if (resource == null) {
            return;
        }
        if (resource instanceof LazyValue<?> lazyValue) {
            if (lazyValue.isInitialized()) {
                closeQuietly(lazyValue.get());
            }
            return;
        }
        if (resource instanceof AutoCloseable closeable) {
            closeQuietly(closeable);
        }
    }

    /**
     * Safely closes all given {@link AutoCloseable} (or {@link Closeable}) instances in order,
     * catching and logging any exception thrown by individual resources.
     *
     * @param closeables the resources to close; may be {@code null} or contain {@code null} elements
     */
    public static void closeAllQuietly(AutoCloseable... closeables) {
        if (closeables == null) {
            return;
        }
        for (AutoCloseable closeable : closeables) {
            closeQuietly(closeable);
        }
    }

    /**
     * Safely closes all given candidate objects in order if they implement {@link AutoCloseable}
     * (or {@link Closeable}), catching and logging any exception thrown by individual resources.
     *
     * @param resources the candidate objects to close; may be {@code null} or contain {@code null} elements
     */
    public static void closeAllQuietly(Object... resources) {
        if (resources == null) {
            return;
        }
        for (Object resource : resources) {
            closeQuietly(resource);
        }
    }

    /**
     * Safely closes all candidate objects in the given {@link Iterable} in order if they implement
     * {@link AutoCloseable} (or {@link Closeable}), catching and logging any exception thrown by
     * individual resources.
     *
     * @param resources the iterable of candidate objects to close; may be {@code null} or contain {@code null} elements
     */
    public static void closeAllQuietly(Iterable<?> resources) {
        if (resources == null) {
            return;
        }
        for (Object resource : resources) {
            closeQuietly(resource);
        }
    }
}
