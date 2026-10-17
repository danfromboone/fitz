package dbf.di;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class HttpResponseBody {
    private static final HttpResponseBody EMPTY = new HttpResponseBody(new byte[0], StandardCharsets.UTF_8);

    private final byte[] bytes;
    private final Charset charset;

    public HttpResponseBody() {
        this(new byte[0], StandardCharsets.UTF_8);
    }

    public HttpResponseBody(String content) {
        this(content, StandardCharsets.UTF_8);
    }

    public HttpResponseBody(String content, Charset charset) {
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
        this.bytes = (content != null) ? content.getBytes(this.charset) : new byte[0];
    }

    public HttpResponseBody(byte[] bytes) {
        this(bytes, StandardCharsets.UTF_8);
    }

    public HttpResponseBody(byte[] bytes, Charset charset) {
        this.charset = (charset != null) ? charset : StandardCharsets.UTF_8;
        this.bytes = (bytes != null) ? bytes.clone() : new byte[0];
    }

    public static HttpResponseBody empty() {
        return EMPTY;
    }

    public static HttpResponseBody of(String content) {
        return (content == null || content.isEmpty()) ? EMPTY : new HttpResponseBody(content);
    }

    public static HttpResponseBody of(byte[] bytes) {
        return (bytes == null || bytes.length == 0) ? EMPTY : new HttpResponseBody(bytes);
    }

    public String asString() {
        return asString(this.charset);
    }

    public String asString(Charset targetCharset) {
        Charset effectiveCharset = (targetCharset != null) ? targetCharset : this.charset;
        return new String(bytes, effectiveCharset);
    }

    public byte[] asBytes() {
        return bytes.clone();
    }

    public Charset getCharset() {
        return charset;
    }

    public int length() {
        return bytes.length;
    }

    public boolean isEmpty() {
        return bytes.length == 0;
    }

    public void writeTo(OutputStream outputStream) throws IOException {
        if (outputStream != null && bytes.length > 0) {
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
