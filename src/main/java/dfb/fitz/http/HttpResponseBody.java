package dfb.fitz.http;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

/**
 * Immutable representation of an HTTP response payload.
 */
public class HttpResponseBody {
    private static final HttpResponseBody EMPTY = new HttpResponseBody(new byte[0], StandardCharsets.UTF_8);

    private final byte[] bytes;
    private final Charset charset;

    /**
     * Creates an empty {@code HttpResponseBody} with UTF-8 encoding.
     */
    public HttpResponseBody() {
        this(new byte[0], StandardCharsets.UTF_8);
    }

    /**
     * Creates an {@code HttpResponseBody} from a string encoded in UTF-8.
     *
     * @param content the string content; if {@code null}, an empty body is created
     */
    public HttpResponseBody(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    /**
     * Creates an {@code HttpResponseBody} from a string using the specified character set.
     *
     * @param content the string content; if {@code null}, an empty body is created
     * @param charset the character set used to encode the string (must not be {@code null})
     * @throws NullPointerException if {@code charset} is {@code null}
     */
    public HttpResponseBody(String content, Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset must not be null");
        this.bytes = (content != null) ? content.getBytes(this.charset) : new byte[0];
    }

    /**
     * Creates an {@code HttpResponseBody} from a byte array with UTF-8 as the default character set.
     *
     * @param bytes the payload bytes (defensively copied); if {@code null}, an empty body is created
     */
    public HttpResponseBody(byte[] bytes) {
        this(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Creates an {@code HttpResponseBody} from a byte array and default character set.
     *
     * @param bytes   the payload bytes (defensively copied); if {@code null}, an empty body is created
     * @param charset the default character set for string conversion (must not be {@code null})
     * @throws NullPointerException if {@code charset} is {@code null}
     */
    public HttpResponseBody(byte[] bytes, Charset charset) {
        this.charset = Objects.requireNonNull(charset, "charset must not be null");
        this.bytes = (bytes != null) ? bytes.clone() : new byte[0];
    }

    /**
     * Returns a shared empty {@code HttpResponseBody} instance.
     *
     * @return the empty response body
     */
    public static HttpResponseBody empty() {
        return EMPTY;
    }

    /**
     * Creates an {@code HttpResponseBody} from a string using UTF-8, or returns {@link #empty()}
     * if the string is {@code null} or empty.
     *
     * @param content the string content
     * @return an {@code HttpResponseBody} containing the string bytes
     */
    public static HttpResponseBody of(String content) {
        return (content == null || content.isEmpty()) ? EMPTY : new HttpResponseBody(content);
    }

    /**
     * Creates an {@code HttpResponseBody} from a byte array, or returns {@link #empty()}
     * if the array is {@code null} or empty.
     *
     * @param bytes the byte array content
     * @return an {@code HttpResponseBody} containing a copy of the bytes
     */
    public static HttpResponseBody of(byte[] bytes) {
        return (bytes == null || bytes.length == 0) ? EMPTY : new HttpResponseBody(bytes);
    }

    /**
     * Decodes this response body as a string using its configured {@link #getCharset() charset}.
     *
     * @return the decoded string
     */
    public String asString() {
        return asString(this.charset);
    }

    /**
     * Decodes this response body as a string using the specified character set.
     *
     * @param targetCharset the character set to decode with (must not be {@code null})
     * @return the decoded string
     * @throws NullPointerException if {@code targetCharset} is {@code null}
     */
    public String asString(Charset targetCharset) {
        Objects.requireNonNull(targetCharset, "targetCharset must not be null");
        return new String(bytes, targetCharset);
    }

    /**
     * Returns a defensive copy of the response body bytes.
     *
     * @return a copy of the byte array
     */
    public byte[] asBytes() {
        return bytes.clone();
    }

    /**
     * Returns the character set associated with this response body.
     *
     * @return the {@link Charset}
     */
    public Charset getCharset() {
        return charset;
    }

    /**
     * Returns the length of this response body in bytes.
     *
     * @return the byte count
     */
    public int length() {
        return bytes.length;
    }

    /**
     * Checks whether this response body has zero bytes.
     *
     * @return {@code true} if empty; {@code false} otherwise
     */
    public boolean isEmpty() {
        return bytes.length == 0;
    }

    /**
     * Writes the response body bytes to the given {@link OutputStream} if non-empty.
     *
     * @param outputStream the output stream to write to (must not be {@code null})
     * @throws NullPointerException if {@code outputStream} is {@code null}
     * @throws IOException          if an I/O error occurs while writing
     */
    public void writeTo(OutputStream outputStream) throws IOException {
        Objects.requireNonNull(outputStream, "outputStream must not be null");
        if (bytes.length > 0) {
            outputStream.write(bytes);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpResponseBody that)) return false;
        return Arrays.equals(bytes, that.bytes);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bytes);
    }

    @Override
    public String toString() {
        return asString();
    }
}
