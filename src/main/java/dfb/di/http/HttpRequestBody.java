package dfb.di.http;

import jakarta.servlet.http.HttpServletRequest;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Encapsulates the body of an {@link HttpRequest}.
 * <p>
 * Note: When backed by an {@link HttpServletRequest}, lazy reading and caching of the body
 * is not thread-safe (consistent with {@link HttpServletRequest}, which is scoped to a single
 * request-handling thread).
 */
public class HttpRequestBody {
    private final HttpServletRequest request;
    private byte[] bytes;
    private Charset charset;

    /**
     * Creates an {@code HttpRequestBody} that lazily reads its content from the given servlet request.
     *
     * @param request the underlying servlet request; may be {@code null}
     */
    public HttpRequestBody(HttpServletRequest request) {
        this.request = request;
    }

    /**
     * Creates an {@code HttpRequestBody} from a string using UTF-8 encoding.
     *
     * @param content the string content; if {@code null}, an empty body is created
     */
    public HttpRequestBody(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    /**
     * Creates an {@code HttpRequestBody} from a string using the specified character encoding.
     *
     * @param content the string content; if {@code null}, an empty body is created
     * @param charset the character set used to encode the string; defaults to UTF-8 if {@code null}
     */
    public HttpRequestBody(String content, Charset charset) {
        this.request = null;
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
        this.bytes = (content != null) ? content.getBytes(this.charset) : new byte[0];
    }

    /**
     * Creates an {@code HttpRequestBody} from a byte array with UTF-8 as the default character set.
     *
     * @param bytes the body bytes (defensively copied); if {@code null}, an empty body is created
     */
    public HttpRequestBody(byte[] bytes) {
        this(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Creates an {@code HttpRequestBody} from a byte array and default character set.
     *
     * @param bytes   the body bytes (defensively copied); if {@code null}, an empty body is created
     * @param charset the default character set for string conversion; defaults to UTF-8 if {@code null}
     */
    public HttpRequestBody(byte[] bytes, Charset charset) {
        this.request = null;
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
        this.bytes = (bytes != null) ? bytes.clone() : new byte[0];
    }

    /**
     * Factory method that creates an {@code HttpRequestBody} backed by an {@link HttpServletRequest}.
     *
     * @param request the servlet request; may be {@code null}
     * @return a new {@code HttpRequestBody}
     */
    public static HttpRequestBody from(HttpServletRequest request) {
        return new HttpRequestBody(request);
    }

    /**
     * Returns the body content decoded as a string using this body's resolved {@link #getCharset() charset}.
     *
     * @return the decoded body string
     * @throws UncheckedIOException if reading from the underlying servlet request fails
     */
    public String asString() {
        return asString(getCharset());
    }

    /**
     * Returns the body content decoded as a string using the specified character set.
     *
     * @param targetCharset the charset to decode with; if {@code null}, {@link #getCharset()} is used
     * @return the decoded body string
     * @throws UncheckedIOException if reading from the underlying servlet request fails
     */
    public String asString(Charset targetCharset) {
        Charset effectiveCharset = (targetCharset != null) ? targetCharset : getCharset();
        return new String(ensureLoaded(), effectiveCharset);
    }

    /**
     * Returns a defensive copy of the raw request body bytes.
     *
     * @return the body bytes
     * @throws UncheckedIOException if reading from the underlying servlet request fails
     */
    public byte[] asBytes() {
        return ensureLoaded().clone();
    }

    /**
     * Returns the character set associated with this request body, resolving it from the
     * underlying servlet request's character encoding if present, or defaulting to UTF-8.
     *
     * @return the resolved {@link Charset}
     */
    public Charset getCharset() {
        if (charset == null) {
            charset = resolveCharset(request);
        }
        return charset;
    }

    /**
     * Returns the length of the request body in bytes.
     *
     * @return the number of bytes in the body
     * @throws UncheckedIOException if reading from the underlying servlet request fails
     */
    public int length() {
        return ensureLoaded().length;
    }

    /**
     * Checks whether the request body has zero bytes.
     *
     * @return {@code true} if the body is empty; {@code false} otherwise
     * @throws UncheckedIOException if reading from the underlying servlet request fails
     */
    public boolean isEmpty() {
        return ensureLoaded().length == 0;
    }

    private byte[] ensureLoaded() {
        if (bytes == null) {
            bytes = readBytes();
        }
        return bytes;
    }

    private byte[] readBytes() {
        if (request == null) {
            return new byte[0];
        }
        Charset effectiveCharset = getCharset();
        try {
            InputStream inputStream = request.getInputStream();
            if (inputStream != null) {
                return inputStream.readAllBytes();
            }
            BufferedReader reader = request.getReader();
            if (reader != null) {
                StringWriter writer = new StringWriter();
                reader.transferTo(writer);
                return writer.toString().getBytes(effectiveCharset);
            }
            return new byte[0];
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read request body", e);
        }
    }

    private static Charset resolveCharset(HttpServletRequest request) {
        if (request != null) {
            String encoding = request.getCharacterEncoding();
            if (encoding != null && !encoding.isBlank()) {
                try {
                    return Charset.forName(encoding);
                } catch (Exception ignored) {
                    // Fallback to UTF-8
                }
            }
        }
        return StandardCharsets.UTF_8;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpRequestBody that)) return false;
        return Arrays.equals(ensureLoaded(), that.ensureLoaded());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(ensureLoaded());
    }

    @Override
    public String toString() {
        return asString();
    }
}
