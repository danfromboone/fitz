package dfb.fitz.property;

import dfb.fitz.Closeables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A decorating {@link MutablePropertySource} that transparently decrypts property values
 * formatted as {@code ENC(<base64>)}.
 * <p>
 * The filesystem path to the encryption key file is resolved dynamically from the underlying
 * {@link PropertySource} using the configured key-path property name
 * (defaulting to {@link #DEFAULT_KEY_PATH_PROPERTY}).
 */
public class DecryptingPropertySource implements MutablePropertySource, AutoCloseable {
    private static final Logger log = LoggerFactory.getLogger(DecryptingPropertySource.class);

    /**
     * Default property key used to look up the filesystem path of the encryption key file.
     */
    public static final String DEFAULT_KEY_PATH_PROPERTY = "property.encryption.key.path";

    private final PropertySource delegate;
    private final String keyPathProperty;
    private final List<PropertyChangeListener> listeners = new CopyOnWriteArrayList<>();

    private volatile Path cachedKeyPath;
    private volatile FileTime cachedKeyModifiedTime;
    private volatile SecretKey cachedSecretKey;
    private volatile Map<String, String> lastDecryptedSnapshot;

    /**
     * Creates a {@code DecryptingPropertySource} that resolves the encryption key file path
     * from {@link #DEFAULT_KEY_PATH_PROPERTY}.
     *
     * @param delegate the underlying property source (must not be {@code null})
     */
    public DecryptingPropertySource(PropertySource delegate) {
        this(delegate, DEFAULT_KEY_PATH_PROPERTY);
    }

    /**
     * Creates a {@code DecryptingPropertySource} that resolves the encryption key file path
     * from the specified property key.
     *
     * @param delegate        the underlying property source (must not be {@code null})
     * @param keyPathProperty the property name holding the path to the encryption key file (must not be {@code null} or blank)
     */
    public DecryptingPropertySource(PropertySource delegate, String keyPathProperty) {
        this.delegate = Objects.requireNonNull(delegate, "delegate must not be null");
        Objects.requireNonNull(keyPathProperty, "keyPathProperty must not be null");
        if (keyPathProperty.isBlank()) {
            throw new IllegalArgumentException("keyPathProperty must not be blank");
        }
        this.keyPathProperty = keyPathProperty;
        this.lastDecryptedSnapshot = decryptMap(delegate.getProperties());

        if (delegate instanceof MutablePropertySource mutableDelegate) {
            mutableDelegate.registerListener(event -> onDelegateChange());
        }
    }

    @Override
    public Map<String, String> getProperties() {
        Map<String, String> decrypted = decryptMap(delegate.getProperties());
        this.lastDecryptedSnapshot = decrypted;
        return decrypted;
    }

    @Override
    public String getProperty(String key) {
        String rawValue = delegate.getProperty(key);
        if (!PropertyEncryptor.isEncrypted(rawValue)) {
            return rawValue;
        }
        return PropertyEncryptor.decrypt(rawValue, resolveSecretKey(delegate));
    }

    @Override
    public void registerListener(PropertyChangeListener listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    /**
     * Unregisters a previously registered property change listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(PropertyChangeListener listener) {
        listeners.remove(listener);
    }

    private synchronized void onDelegateChange() {
        Map<String, String> latest;
        try {
            latest = decryptMap(delegate.getProperties());
        } catch (Exception e) {
            log.warn("Failed to decrypt updated properties; keeping previous snapshot", e);
            return;
        }

        Map<String, String> previous = this.lastDecryptedSnapshot;
        List<PropertyChange> changes = diff(previous, latest);
        if (changes.isEmpty()) {
            return;
        }

        this.lastDecryptedSnapshot = latest;
        PropertyChangeEvent event = new PropertyChangeEvent(changes);
        for (PropertyChangeListener listener : listeners) {
            try {
                listener.onPropertyChange(event);
            } catch (Exception e) {
                log.error("Error notifying PropertyChangeListener in DecryptingPropertySource", e);
            }
        }
    }

    private Map<String, String> decryptMap(Map<String, String> rawProps) {
        if (rawProps == null || rawProps.isEmpty()) {
            return Collections.emptyMap();
        }

        SecretKey key = null;
        Map<String, String> result = new LinkedHashMap<>(rawProps.size());
        for (Map.Entry<String, String> entry : rawProps.entrySet()) {
            String value = entry.getValue();
            if (PropertyEncryptor.isEncrypted(value)) {
                if (key == null) {
                    key = resolveSecretKey(rawProps.get(keyPathProperty));
                }
                value = PropertyEncryptor.decrypt(value, key);
            }
            result.put(entry.getKey(), value);
        }
        return Collections.unmodifiableMap(result);
    }

    private SecretKey resolveSecretKey(PropertySource source) {
        return resolveSecretKey(source.getProperty(keyPathProperty));
    }

    private synchronized SecretKey resolveSecretKey(String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException(
                    "Encrypted property encountered, but key file path property '" + keyPathProperty + "' is not configured"
            );
        }

        Path path = Path.of(configuredPath.trim());
        FileTime lastModified = getLastModifiedTime(path);

        if (cachedSecretKey != null
                && path.equals(cachedKeyPath)
                && Objects.equals(lastModified, cachedKeyModifiedTime)) {
            return cachedSecretKey;
        }

        SecretKey loaded = PropertyEncryptor.loadKey(path);
        this.cachedKeyPath = path;
        this.cachedKeyModifiedTime = lastModified;
        this.cachedSecretKey = loaded;
        return loaded;
    }

    private static FileTime getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException ignored) {
            return null;
        }
    }

    private static List<PropertyChange> diff(Map<String, String> previous, Map<String, String> latest) {
        Set<String> allKeys = new LinkedHashSet<>(previous.keySet());
        allKeys.addAll(latest.keySet());

        List<PropertyChange> changes = new ArrayList<>();
        for (String key : allKeys) {
            String oldValue = previous.get(key);
            String newValue = latest.get(key);
            if (!Objects.equals(oldValue, newValue)) {
                changes.add(new PropertyChange(key, oldValue, newValue));
            }
        }
        return changes;
    }

    /**
     * Closes the underlying delegate if it implements {@link AutoCloseable}.
     */
    @Override
    public void close() {
        Closeables.closeQuietly(delegate);
    }
}
