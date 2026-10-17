package dfb.di.http;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates an HTTP request URI, providing access to its path, query parameters,
 * regex matching, and named path parameter extraction.
 */
public class HttpUri {
    private final URI uri;
    private final HttpQueryParams queryParams;

    /**
     * Creates an {@code HttpUri} by parsing the given URI string.
     *
     * @param uri the raw URI string; may be {@code null}
     * @throws IllegalArgumentException if the string violates RFC 2396 URI syntax
     */
    public HttpUri(String uri) {
        this.uri = (uri != null) ? URI.create(uri) : null;
        this.queryParams = new HttpQueryParams(this.uri);
    }

    /**
     * Creates an {@code HttpUri} by combining a base URI string and a query string.
     *
     * @param uri   the base URI string; may be {@code null}
     * @param query the raw query string (with or without a leading {@code '?'}); may be {@code null}
     * @throws IllegalArgumentException if the combined URI violates RFC 2396 syntax
     */
    public HttpUri(String uri, String query) {
        this(combineUriAndQuery(uri, query));
    }

    private static String combineUriAndQuery(String uri, String query) {
        if (query == null || query.isEmpty()) {
            return uri;
        }
        String base = (uri != null) ? uri : "";
        String normalizedQuery = query.startsWith("?") ? query.substring(1) : query;
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + normalizedQuery;
    }

    /**
     * Constructs an {@code HttpUri} from a Jakarta {@link HttpServletRequest}.
     *
     * @param request the servlet request (must not be {@code null})
     * @return the constructed {@code HttpUri}
     * @throws NullPointerException if {@code request} is {@code null}
     */
    public static HttpUri from(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        StringBuffer requestUrl = request.getRequestURL();
        String base = (requestUrl != null) ? requestUrl.toString() : request.getRequestURI();
        if (base == null) {
            base = "";
        }

        return new HttpUri(base, request.getQueryString());
    }

    /**
     * Returns the underlying {@link URI}.
     *
     * @return the parsed {@link URI}, or {@code null} if constructed with {@code null}
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns the decoded path component of this URI.
     *
     * @return the URI path, or {@code null} if no URI is present
     */
    public String getPath() {
        return (uri != null) ? uri.getPath() : null;
    }

    /**
     * Returns the decoded query component of this URI.
     *
     * @return the decoded query string, or {@code null} if no query is present
     */
    public String getQuery() {
        return (uri != null) ? uri.getQuery() : null;
    }

    /**
     * Returns the raw (undecoded) query component of this URI.
     *
     * @return the raw query string, or {@code null} if no query is present
     */
    public String getRawQuery() {
        return (uri != null) ? uri.getRawQuery() : null;
    }

    /**
     * Returns the parsed {@link HttpQueryParams} for this URI.
     *
     * @return the query parameters
     */
    public HttpQueryParams getQueryParams() {
        return queryParams;
    }

    /**
     * Returns the full URI string representation.
     *
     * @return the raw URI string, or {@code null} if no URI is present
     */
    public String getRawUri() {
        return (uri != null) ? uri.toString() : null;
    }

    /**
     * Tests whether the URI path (or full URI string) matches the given compiled regex {@link Pattern}.
     *
     * @param pattern the regular expression pattern to match against
     * @return {@code true} if the path or full URI matches the pattern; {@code false} otherwise
     */
    public boolean matches(Pattern pattern) {
        if (pattern == null || uri == null) {
            return false;
        }
        String path = getPath();
        return (path != null && pattern.matcher(path).matches())
                || pattern.matcher(uri.toString()).matches();
    }

    /**
     * Tests whether the URI path (or full URI string) matches the given regular expression string.
     *
     * @param regex the regular expression to match against
     * @return {@code true} if the path or full URI matches the regex; {@code false} otherwise
     */
    public boolean matches(String regex) {
        return (regex != null) && matches(Pattern.compile(regex));
    }

    /**
     * Matches {@code pattern} against the URI path (or full URI) and extracts any named
     * regex capturing groups (e.g., {@code (?<id>[^/]+)}) into an unmodifiable map.
     *
     * @param pattern the compiled regular expression with named capturing groups
     * @return an unmodifiable map of group names to matched values, or an empty map if it does not match
     */
    public Map<String, String> extractPathParams(Pattern pattern) {
        if (pattern == null || uri == null) {
            return Collections.emptyMap();
        }
        String path = getPath();
        Matcher matcher = (path != null) ? pattern.matcher(path) : null;
        if (matcher == null || !matcher.matches()) {
            matcher = pattern.matcher(uri.toString());
            if (!matcher.matches()) {
                return Collections.emptyMap();
            }
        }
        Map<String, Integer> namedGroups = matcher.namedGroups();
        if (namedGroups.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<String, String> extracted = new LinkedHashMap<>(namedGroups.size());
        for (String groupName : namedGroups.keySet()) {
            extracted.put(groupName, matcher.group(groupName));
        }
        return Collections.unmodifiableMap(extracted);
    }

    /**
     * Matches {@code regex} against the URI path (or full URI) and extracts any named
     * regex capturing groups (e.g., {@code (?<id>[^/]+)}) into an unmodifiable map.
     *
     * @param regex the regular expression string with named capturing groups
     * @return an unmodifiable map of group names to matched values, or an empty map if it does not match
     */
    public Map<String, String> extractPathParams(String regex) {
        return (regex != null) ? extractPathParams(Pattern.compile(regex)) : Collections.emptyMap();
    }

    @Override
    public String toString() {
        return (uri != null) ? uri.toString() : null;
    }
}
