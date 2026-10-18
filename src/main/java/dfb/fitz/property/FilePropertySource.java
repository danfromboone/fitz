package dfb.fitz.property;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Supplier;

/**
 * A {@link PropertySource} that loads key-value properties from a {@code .properties} file
 * on the filesystem or classpath (UTF-8 encoded).
 */
public class FilePropertySource implements PropertySource {
    private static final Logger log = LoggerFactory.getLogger(FilePropertySource.class);

    private final Supplier<Map<String, String>> loader;

    /**
     * Creates a {@code FilePropertySource} that reads from the specified file path or classpath resource.
     *
     * @param filePath the filesystem path or classpath resource location (must not be {@code null})
     * @throws NullPointerException if {@code filePath} is {@code null}
     */
    public FilePropertySource(String filePath) {
        Objects.requireNonNull(filePath, "filePath must not be null");
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        this.loader = () -> loadProperties(filePath, classLoader);
    }

    /**
     * Creates a {@code FilePropertySource} that reads from the specified {@link File}.
     *
     * @param file the properties file (must not be {@code null})
     * @throws NullPointerException if {@code file} is {@code null}
     */
    public FilePropertySource(File file) {
        this(Objects.requireNonNull(file, "file must not be null").toPath());
    }

    /**
     * Creates a {@code FilePropertySource} that reads from the specified {@link Path}.
     *
     * @param path the path to the properties file (must not be {@code null})
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public FilePropertySource(Path path) {
        Objects.requireNonNull(path, "path must not be null");
        this.loader = () -> loadFromPath(path);
    }

    private static Map<String, String> loadProperties(String filePath, ClassLoader capturedClassLoader) {
        Path path = Path.of(filePath);
        if (Files.exists(path)) {
            return loadFromPath(path);
        }

        String resourceName = filePath.startsWith("/") ? filePath.substring(1) : filePath;
        ClassLoader cl = (capturedClassLoader != null)
                ? capturedClassLoader
                : FilePropertySource.class.getClassLoader();
        URL resourceUrl = (cl != null) ? cl.getResource(resourceName) : null;
        if (resourceUrl != null) {
            if ("file".equalsIgnoreCase(resourceUrl.getProtocol())) {
                try {
                    return loadFromPath(Path.of(resourceUrl.toURI()));
                } catch (Exception ignored) {
                    // Fall back to URLConnection stream
                }
            }
            try {
                URLConnection connection = resourceUrl.openConnection();
                connection.setUseCaches(false);
                try (InputStream in = connection.getInputStream();
                     Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    log.debug("Loading properties from classpath resource: {}", resourceName);
                    return parseReader(reader);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to read properties from classpath resource: " + filePath, e);
            }
        }

        throw new UncheckedIOException(new IOException("Properties file not found: " + filePath));
    }

    private static Map<String, String> loadFromPath(Path path) {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            log.debug("Loading properties from file path: {}", path);
            return parseReader(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read properties from file: " + path, e);
        }
    }

    private static Map<String, String> parseReader(Reader reader) throws IOException {
        Properties props = new Properties();
        props.load(reader);
        Map<String, String> map = new LinkedHashMap<>(props.size());
        for (String name : props.stringPropertyNames()) {
            map.put(name, props.getProperty(name));
        }
        return Collections.unmodifiableMap(map);
    }

    /**
     * Loads and returns an unmodifiable map of properties from the configured file or resource.
     *
     * @return the loaded properties map
     * @throws UncheckedIOException if the file cannot be found or read
     */
    @Override
    public Map<String, String> getProperties() {
        return loader.get();
    }
}
