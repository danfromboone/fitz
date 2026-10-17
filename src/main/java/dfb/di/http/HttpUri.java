package dfb.di.http;

import dfb.di.Validate;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Encapsulates an HTTP request URI, providing access to its path, query parameters,
 * regex matching, and named path parameter extraction.
 */
public class HttpUri {
    private static final Map<String, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

    private final URI uri;
    private final HttpQueryParams queryParams;

    /**
     * Creates an {@code HttpUri} by parsing the given URI string.
     *
     * @param uri the raw URI string (must not be {@code null} or blank)
     * @throws NullPointerException     if {@code uri} is {@code null}
     * @throws IllegalArgumentException if {@code uri} is blank or violates RFC 2396 URI syntax
     */
    public HttpUri(String uri) {
        this.uri = URI.create(Validate.trimmedNonBlank(uri, "uri"));
        this.queryParams = new HttpQueryParams(this.uri);
    }

    /**
     * Creates an {@code HttpUri} by combining a base URI string and an optional query string.
     *
     * @param uri   the base URI string (must not be {@code null} or blank)
     * @param query the raw query string (with or without a leading {@code '?'}); may be {@code null} or empty
     * @throws NullPointerException     if {@code uri} is {@code null}
     * @throws IllegalArgumentException if {@code uri} is blank or the combined URI violates RFC 2396 syntax
     */
    public HttpUri(String uri, String query) {
        this(combineUriAndQuery(uri, query));
    }

    private static String combineUriAndQuery(String uri, String query) {
        String base = Validate.trimmedNonBlank(uri, "uri");
        String trimmedQuery = Validate.trimToEmpty(query);
        if (trimmedQuery.isEmpty()) {
            return base;
        }
        String normalizedQuery = trimmedQuery.startsWith("?")
                ? trimmedQuery.substring(1).trim()
                : trimmedQuery;
        if (normalizedQuery.isEmpty()) {
            return base;
        }
        String separator = base.contains("?") ? "&" : "?";
        return base + separator + normalizedQuery;
    }

    /**
     * Constructs an {@code HttpUri} from a Jakarta {@link HttpServletRequest}.
     *
     * @param request the servlet request (must not be {@code null})
     * @return the constructed {@code HttpUri}
     * @throws NullPointerException     if {@code request} or its URI is {@code null}
     * @throws IllegalArgumentException if the request URI is blank or invalid
     */
    public static HttpUri from(HttpServletRequest request) {
        Validate.nonNull(request, "request");
        StringBuffer requestUrl = request.getRequestURL();
        String base = (requestUrl != null) ? requestUrl.toString() : request.getRequestURI();
        return new HttpUri(base, request.getQueryString());
    }

    private static Pattern compileRegex(String regex) {
        String trimmed = Validate.trimmedNonBlank(regex, "regex");
        return PATTERN_CACHE.computeIfAbsent(trimmed, Pattern::compile);
    }

    /**
     * Returns the underlying {@link URI}.
     *
     * @return the parsed {@link URI} (never {@code null})
     */
    public URI getUri() {
        return uri;
    }

    /**
     * Returns the decoded path component of this URI.
     *
     * @return the URI path
     */
    public String getPath() {
        return uri.getPath();
    }

    /**
     * Returns the decoded query component of this URI.
     *
     * @return the decoded query string, or {@code null} if no query is present
     */
    public String getQuery() {
        return uri.getQuery();
    }

    /**
     * Returns the raw (undecoded) query component of this URI.
     *
     * @return the raw query string, or {@code null} if no query is present
     */
    public String getRawQuery() {
        return uri.getRawQuery();
    }

    /**
     * Returns the parsed {@link HttpQueryParams} for this URI.
     *
     * @return the query parameters (never {@code null})
     */
    public HttpQueryParams getQueryParams() {
        return queryParams;
    }

    /**
     * Returns the full URI string representation.
     *
     * @return the raw URI string (never {@code null})
     */
    public String getRawUri() {
        return uri.toString();
    }

    /**
     * Tests whether the URI path (or full URI string) matches the given compiled regex {@link Pattern}.
     *
     * @param pattern the regular expression pattern to match against (must not be {@code null})
     * @return {@code true} if the path or full URI matches the pattern; {@code false} otherwise
     * @throws NullPointerException if {@code pattern} is {@code null}
     */
    public boolean matches(Pattern pattern) {
        Validate.nonNull(pattern, "pattern");
        String path = getPath();
        return (path != null && pattern.matcher(path).matches())
                || pattern.matcher(uri.toString()).matches();
    }

    /**
     * Tests whether the URI path (or full URI string) matches the given regular expression string.
     *
     * @param regex the regular expression to match against (must not be {@code null} or blank)
     * @return {@code true} if the path or full URI matches the regex; {@code false} otherwise
     * @throws NullPointerException     if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public boolean matches(String regex) {
        return matches(compileRegex(regex));
    }

    /**
     * Matches {@code pattern} against the URI path (or full URI) and extracts any named
     * regex capturing groups (e.g., {@code (?<id>[^/]+)}) into an unmodifiable map.
     *
     * @param pattern the compiled regular expression with named capturing groups (must not be {@code null})
     * @return an unmodifiable map of group names to matched values, or an empty map if it does not match
     * @throws NullPointerException if {@code pattern} is {@code null}
     */
    public Map<String, String> extractPathParams(Pattern pattern) {
        Validate.nonNull(pattern, "pattern");
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
     * @param regex the regular expression string with named capturing groups (must not be {@code null} or blank)
     * @return an unmodifiable map of group names to matched values, or an empty map if it does not match
     * @throws NullPointerException     if {@code regex} is {@code null}
     * @throws IllegalArgumentException if {@code regex} is blank
     */
    public Map<String, String> extractPathParams(String regex) {
        return extractPathParams(compileRegex(regex));
    }

    @Override
    public String toString() {
        return uri.toString();
    }
}
