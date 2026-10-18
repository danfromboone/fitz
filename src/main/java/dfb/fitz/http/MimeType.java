package dfb.fitz.http;

import dfb.fitz.Validate;
import lombok.Getter;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;

/**
 * Immutable representation of an HTTP MIME media type (e.g., {@code "application/json"} or
 * {@code "text/plain; charset=UTF-8"}), along with constants for common standard media types.
 */
@Getter
public final class MimeType {
    /** {@code *&#47;*} wildcard media type. */
    public static final MimeType ALL = new MimeType("*", "*");

    // application/*
    /** {@code application/json}. */
    public static final MimeType APPLICATION_JSON = new MimeType("application", "json");
    /** {@code application/json; charset=UTF-8}. */
    public static final MimeType APPLICATION_JSON_UTF8 = new MimeType("application", "json", StandardCharsets.UTF_8);
    /** {@code application/xml}. */
    public static final MimeType APPLICATION_XML = new MimeType("application", "xml");
    /** {@code application/x-www-form-urlencoded}. */
    public static final MimeType APPLICATION_FORM_URLENCODED = new MimeType("application", "x-www-form-urlencoded");
    /** {@code application/octet-stream}. */
    public static final MimeType APPLICATION_OCTET_STREAM = new MimeType("application", "octet-stream");
    /** {@code application/pdf}. */
    public static final MimeType APPLICATION_PDF = new MimeType("application", "pdf");

    // multipart/*
    /** {@code multipart/form-data}. */
    public static final MimeType MULTIPART_FORM_DATA = new MimeType("multipart", "form-data");

    // text/*
    /** {@code text/plain}. */
    public static final MimeType TEXT_PLAIN = new MimeType("text", "plain");
    /** {@code text/plain; charset=UTF-8}. */
    public static final MimeType TEXT_PLAIN_UTF8 = new MimeType("text", "plain", StandardCharsets.UTF_8);
    /** {@code text/html}. */
    public static final MimeType TEXT_HTML = new MimeType("text", "html");
    /** {@code text/html; charset=UTF-8}. */
    public static final MimeType TEXT_HTML_UTF8 = new MimeType("text", "html", StandardCharsets.UTF_8);
    /** {@code text/css}. */
    public static final MimeType TEXT_CSS = new MimeType("text", "css");
    /** {@code text/csv}. */
    public static final MimeType TEXT_CSV = new MimeType("text", "csv");
    /** {@code text/xml}. */
    public static final MimeType TEXT_XML = new MimeType("text", "xml");

    // image/*
    /** {@code image/png}. */
    public static final MimeType IMAGE_PNG = new MimeType("image", "png");
    /** {@code image/jpeg}. */
    public static final MimeType IMAGE_JPEG = new MimeType("image", "jpeg");
    /** {@code image/gif}. */
    public static final MimeType IMAGE_GIF = new MimeType("image", "gif");
    /** {@code image/svg+xml}. */
    public static final MimeType IMAGE_SVG_XML = new MimeType("image", "svg+xml");

    private final String type;
    private final String subtype;
    private final Charset charset;

    /**
     * Creates a {@code MimeType} with the given primary type and subtype and no explicit charset.
     *
     * @param type    the primary type, e.g. {@code "application"} or {@code "text"} (must not be {@code null} or blank)
     * @param subtype the subtype, e.g. {@code "json"} or {@code "plain"} (must not be {@code null} or blank)
     * @throws NullPointerException     if {@code type} or {@code subtype} is {@code null}
     * @throws IllegalArgumentException if {@code type} or {@code subtype} is blank or contains {@code '/'}
     */
    public MimeType(String type, String subtype) {
        this(type, subtype, null);
    }

    /**
     * Creates a {@code MimeType} with the given primary type, subtype, and optional {@link Charset}.
     *
     * @param type    the primary type, e.g. {@code "application"} or {@code "text"} (must not be {@code null} or blank)
     * @param subtype the subtype, e.g. {@code "json"} or {@code "plain"} (must not be {@code null} or blank)
     * @param charset the optional character set; may be {@code null}
     * @throws NullPointerException     if {@code type} or {@code subtype} is {@code null}
     * @throws IllegalArgumentException if {@code type} or {@code subtype} is blank or contains {@code '/'}
     */
    public MimeType(String type, String subtype, Charset charset) {
        String normalizedType = Validate.trimmedNonBlank(type, "type").toLowerCase(Locale.ROOT);
        String normalizedSubtype = Validate.trimmedNonBlank(subtype, "subtype").toLowerCase(Locale.ROOT);
        if (normalizedType.indexOf('/') >= 0) {
            throw new IllegalArgumentException("type must not contain '/': " + type);
        }
        if (normalizedSubtype.indexOf('/') >= 0) {
            throw new IllegalArgumentException("subtype must not contain '/': " + subtype);
        }
        if ("*".equals(normalizedType) && !"*".equals(normalizedSubtype)) {
            throw new IllegalArgumentException("wildcard type '*' requires wildcard subtype '*': " + type + "/" + subtype);
        }
        this.type = normalizedType;
        this.subtype = normalizedSubtype;
        this.charset = charset;
    }

    /**
     * Factory method creating a {@code MimeType} with the given type and subtype.
     *
     * @param type    the primary type (must not be {@code null} or blank)
     * @param subtype the subtype (must not be {@code null} or blank)
     * @return the created {@code MimeType}
     */
    public static MimeType of(String type, String subtype) {
        return new MimeType(type, subtype, null);
    }

    /**
     * Factory method creating a {@code MimeType} with the given type, subtype, and optional charset.
     *
     * @param type    the primary type (must not be {@code null} or blank)
     * @param subtype the subtype (must not be {@code null} or blank)
     * @param charset the optional character set; may be {@code null}
     * @return the created {@code MimeType}
     */
    public static MimeType of(String type, String subtype, Charset charset) {
        return new MimeType(type, subtype, charset);
    }

    /**
     * Parses a MIME type string (such as a {@code Content-Type} header value like
     * {@code "application/json"} or {@code "text/plain; charset=UTF-8"}) into a {@code MimeType}.
     *
     * @param value the MIME type string to parse (must not be {@code null} or blank)
     * @return the parsed {@code MimeType}
     * @throws NullPointerException     if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is blank, malformed, or specifies an unsupported charset
     */
    public static MimeType parse(String value) {
        String trimmed = Validate.trimmedNonBlank(value, "mimeType");
        String[] parts = trimmed.split(";");
        String mediaRange = parts[0].trim();
        int slashIdx = mediaRange.indexOf('/');
        if (slashIdx <= 0 || slashIdx == mediaRange.length() - 1 || mediaRange.indexOf('/', slashIdx + 1) >= 0) {
            throw new IllegalArgumentException("Invalid MIME type: " + value);
        }

        String parsedType = mediaRange.substring(0, slashIdx);
        String parsedSubtype = mediaRange.substring(slashIdx + 1);
        Charset parsedCharset = null;

        for (int i = 1; i < parts.length; i++) {
            String param = parts[i].trim();
            int eqIdx = param.indexOf('=');
            if (eqIdx > 0) {
                String paramName = param.substring(0, eqIdx).trim();
                String paramValue = unquote(param.substring(eqIdx + 1).trim());
                if ("charset".equalsIgnoreCase(paramName) && !paramValue.isEmpty()) {
                    try {
                        parsedCharset = Charset.forName(paramValue);
                    } catch (Exception e) {
                        throw new IllegalArgumentException("Invalid charset in MIME type: " + value, e);
                    }
                }
            }
        }

        return new MimeType(parsedType, parsedSubtype, parsedCharset);
    }

    private static String unquote(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1).trim();
        }
        return s;
    }

    /**
     * Returns a copy of this {@code MimeType} with the specified {@link Charset}.
     *
     * @param newCharset the character set to associate; may be {@code null}
     * @return a {@code MimeType} with the specified charset
     */
    public MimeType withCharset(Charset newCharset) {
        if (Objects.equals(this.charset, newCharset)) {
            return this;
        }
        return new MimeType(this.type, this.subtype, newCharset);
    }

    /**
     * Returns the base media type without parameters (i.e., {@code "type/subtype"}).
     *
     * @return the base media type string
     */
    public String getBaseType() {
        return type + "/" + subtype;
    }

    /**
     * Checks whether this {@code MimeType} matches the given {@code other} {@code MimeType}
     * in terms of type and subtype (supporting {@code "*"} wildcards and ignoring charset parameters).
     *
     * @param other the other {@code MimeType} to compare against; if {@code null}, returns {@code false}
     * @return {@code true} if compatible; {@code false} otherwise
     */
    public boolean matches(MimeType other) {
        if (other == null) {
            return false;
        }
        if ("*".equals(this.type) || "*".equals(other.type)) {
            return true;
        }
        if (!this.type.equals(other.type)) {
            return false;
        }
        return "*".equals(this.subtype) || "*".equals(other.subtype) || this.subtype.equals(other.subtype);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MimeType that)) return false;
        return type.equals(that.type)
                && subtype.equals(that.subtype)
                && Objects.equals(charset, that.charset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, subtype, charset);
    }

    @Override
    public String toString() {
        return (charset != null)
                ? type + "/" + subtype + "; charset=" + charset.name()
                : type + "/" + subtype;
    }
}
