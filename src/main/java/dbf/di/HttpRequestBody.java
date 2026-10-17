package dbf.di;

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

    public HttpRequestBody(HttpServletRequest request) {
        this.request = request;
    }

    public HttpRequestBody(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    public HttpRequestBody(String content, Charset charset) {
        this.request = null;
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
        this.bytes = (content != null) ? content.getBytes(this.charset) : new byte[0];
    }

    public HttpRequestBody(byte[] bytes) {
        this(bytes, StandardCharsets.UTF_8);
    }

    public HttpRequestBody(byte[] bytes, Charset charset) {
        this.request = null;
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
        this.bytes = (bytes != null) ? bytes.clone() : new byte[0];
    }

    public static HttpRequestBody from(HttpServletRequest request) {
        return new HttpRequestBody(request);
    }

    public String asString() {
        return asString(getCharset());
    }

    public String asString(Charset targetCharset) {
        Charset effectiveCharset = (targetCharset != null) ? targetCharset : getCharset();
        return new String(ensureLoaded(), effectiveCharset);
    }

    public byte[] asBytes() {
        return ensureLoaded().clone();
    }

    public Charset getCharset() {
        if (charset == null) {
            charset = resolveCharset(request);
        }
        return charset;
    }

    public int length() {
        return ensureLoaded().length;
    }

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
