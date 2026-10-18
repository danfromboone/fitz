# fitz

Named after **Walter fitz Alan**, the 12th-century High Steward of Scotland, **fitz** is a small, tight, and helpful pure-Java Dependency Injection (DI), web, HTTP, and live-configuration toolkit built directly on top of Jakarta Servlets.

Like a good steward, **fitz** doesn't desire to run things for you — it helps *you* run them.

---

## Philosophy

Instead of relying on runtime reflection, classpath scanning, annotation processors, or dynamic proxies, **fitz** uses **pure Java constructor injection** wired in an explicit composition root (`ApplicationContext`):

- **Compile-time safety:** Missing or mismatched dependencies fail at compile time rather than at runtime startup.
- **Zero magic:** Navigating the dependency graph is just standard IDE *Go to Definition* / *Find Usages*.
- **Fast startup & simple testing:** Domain services, repositories, and controllers are plain Java objects with no framework annotations or servlet API coupling.
- **Modular sub-contexts:** Related infrastructure concerns (such as data-access wiring and live reconfiguration in `DataContext`) can be encapsulated in sub-contexts and composed into `ApplicationContext`.

---

## Architecture & Package Structure

### 1. Core Utilities (`dfb.fitz`)
- **`LazyValue<T>`**: Thread-safe double-checked locking `Supplier<T>` wrapper for on-demand initialization when needed.
- **`Closeables`**: Shutdown utility (`closeQuietly`, `closeAllQuietly`) for safely closing `Closeable`, `AutoCloseable`, initialized `LazyValue` instances, or arbitrary component references while catching and logging exceptions.
- **`Validate`**: Argument validation and normalization utility (`nonNull`, `trimmedNonBlank`, `trimToEmpty`, `inRange`, `positive`).

### 2. Web Dispatch (`dfb.fitz.web`)
- **`DispatcherServlet`**: Front-controller `HttpServlet` registered programmatically by the application context; dispatches incoming requests to the first matching `Controller`.
- **`Controller`**: Simple interface (`canHandle(HttpRequest)` and `handle(HttpRequest)`) for handling web requests without coupling to Jakarta Servlet APIs.

### 3. HTTP Abstractions & Client (`dfb.fitz.http`)
Provides clean HTTP value objects over Jakarta Servlet APIs and outbound HTTP client calls:

- **`HttpRequest` / `HttpRequestBody`**: High-level request wrapper with URI, method, headers, query parameters, and lazy body reading.
- **`HttpResponse` / `HttpResponseBody` / `HttpStatus`**: Value objects representing HTTP responses, status codes, headers, and payloads.
- **`HttpUri` / `HttpQueryParams` / `HttpHeaders` / `HttpMethod`**: Rich utilities for URI regex matching, named path-parameter extraction (`(?<id>[^/]+)`), decoded multi-value query parameters, and case-insensitive headers.
- **`HttpClient` / `ApacheHttpClient` / `HttpClientConfig`**: Outbound HTTP client abstraction operating on `HttpRequest` and `HttpResponse`, backed by Apache HttpClient 5's `PoolingHttpClientConnectionManager` with `baseUri` resolution and live runtime reconfiguration of connection timeouts, response timeouts, pool sizes, and TTLs.

### 4. Live-Configurable JDBC DataSource (`dfb.fitz.jdbc`)
- **`MutableDataSource` / `DatabaseConfig`**: A `javax.sql.DataSource` (`AutoCloseable`) delegating to HikariCP's `HikariDataSource`. Supports live runtime reconfiguration via `setDatabaseConfig(DatabaseConfig)` — updating credentials, timeouts, and pool sizes in-place via `HikariConfigMXBean` (and soft-evicting connections when credentials rotate) when the JDBC URL is unchanged, or cleanly closing and replacing the pool when the JDBC URL changes.

### 5. Live-Configurable Property Subsystem (`dfb.fitz.property`)
A composable, decorator-based configuration subsystem supporting:

- **Pluggable backends (`PropertySource`, `FilePropertySource`):** Load key-value properties from filesystem paths, classpath resources, or custom sources (e.g., databases, REST APIs, in-memory maps).
- **Layered overrides (`CompositePropertySource`):** Stack multiple `PropertySource` instances from lowest precedence (e.g., `default.properties`) to highest precedence (e.g., `local.properties`). Observes any mutable child sources and emits change events only when effective merged values change.
- **Independent polling & live events (`MutablePropertySource`, `PollingPropertySource`):** Poll individual property sources at their own intervals (`Duration`), diff snapshots, and notify listeners (including regex-filtered key listeners like `registerListener("database\\..*", () -> ...)`).
- **Transparent decryption (`DecryptingPropertySource`, `PropertyEncryptor`):** Automatically decrypts `ENC(<base64>)` property values using `AES/GCM/NoPadding`. The path to the external OS encryption key file is resolved dynamically from configuration via `property.encryption.key.path`.

### 6. Sample Application (`dfb.demo` & `dfb.demo.domain`)
Demonstrates how an application uses **fitz**:

- **`ApplicationContext`**: A `@WebListener` (`ServletContextListener`) that acts as the composition root—wiring the property pipeline, `DataContext`, repositories (`FooRepository`, `GooRepository`), services (`FooService`, `GooService`), controllers (`FooController`), and registering `DispatcherServlet` with the servlet container.
- **`DataContext`**: Demonstrates live, thread-safe reconfiguration. `DataContext` registers a `"database\\..*"` listener on `MutablePropertySource` and updates `MutableDataSource` with a new `DatabaseConfig` whenever database properties change—without re-instantiating repositories or services.
- **`Foo` / `Goo` (`dfb.demo.domain`)**: Sample domain model classes used by the repositories and services.

```text
default.properties ──┐
                     ├──► CompositePropertySource ──► DecryptingPropertySource ──► DataContext
local.properties   ──┘                                                                 │
(PollingPropertySource)                                                                ▼
                                                                             MutableDataSource
                                                                              (DatabaseConfig)
                                                                                 ┌─────┴─────┐
                                                                                 ▼           ▼
                                                                           FooRepository GooRepository
                                                                                 │           │
                                                                                 ▼           ▼
                                                                            FooService   GooService
                                                                                 │
                                                                                 ▼
                                                                           FooController
                                                                                 │
                                                                                 ▼
                                                                         DispatcherServlet
```

---

## Encrypted Properties Usage

1. **Generate a 256-bit AES key file:**
   ```java
   String key = PropertyEncryptor.generateKeyString();
   Files.writeString(Path.of("/etc/fitz/master.key"), key);
   ```
2. **Encrypt a sensitive property value:**
   ```java
   String encrypted = PropertyEncryptor.encrypt("my-secret-password", Path.of("/etc/fitz/master.key"));
   // Produces: ENC(...)
   ```
3. **Configure in `.properties`:**
   ```properties
   property.encryption.key.path=/etc/fitz/master.key
   database.password=ENC(...)
   ```
   When wrapped with `DecryptingPropertySource`, `propertySource.getProperty("database.password")` transparently returns `"my-secret-password"`.

---

## Building and Running

### Prerequisites
- **JDK 27**
- **Apache Tomcat 11+** (or any Jakarta Servlet 6.1 compatible container)

### Run Unit & Integration Tests
The project uses [Spock Framework](https://spockframework.org/) for unit and integration testing:

```bash
./gradlew test
```

### Build WAR Archive
```bash
./gradlew war
```
The packaged WAR is generated at `build/libs/fitz-1.0-SNAPSHOT.war`.

### Sample Endpoint
Once deployed to Tomcat at the root context (`/`):
```text
GET http://localhost:8080/foo?greeting=Hello%20World&flag
```
Response (`200 OK`, `text/plain; charset=UTF-8`):
```text
Request params: {greeting=[Hello World], flag=[]}
```
