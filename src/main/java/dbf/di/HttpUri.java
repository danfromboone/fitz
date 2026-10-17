package dbf.di;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HttpUri {
    private final URI uri;
    private final QueryParams queryParams;

    public HttpUri(String uri) {
        this.uri = (uri != null) ? URI.create(uri) : null;
        this.queryParams = new QueryParams(this.uri);
    }

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

    public static HttpUri from(HttpServletRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        StringBuffer requestUrl = request.getRequestURL();
        String base = (requestUrl != null) ? requestUrl.toString() : request.getRequestURI();
        if (base == null) {
            base = "";
        }

        return new HttpUri(base, request.getQueryString());
    }

    public URI getUri() {
        return uri;
    }

    public String getPath() {
        return (uri != null) ? uri.getPath() : null;
    }

    public String getQuery() {
        return (uri != null) ? uri.getQuery() : null;
    }

    public String getRawQuery() {
        return (uri != null) ? uri.getRawQuery() : null;
    }

    public QueryParams getQueryParams() {
        return queryParams;
    }

    public String getRawUri() {
        return (uri != null) ? uri.toString() : null;
    }

    public boolean matches(Pattern pattern) {
        if (pattern == null || uri == null) {
            return false;
        }
        String path = getPath();
        return (path != null && pattern.matcher(path).matches())
                || pattern.matcher(uri.toString()).matches();
    }

    public boolean matches(String regex) {
        return (regex != null) && matches(Pattern.compile(regex));
    }

    /**
     * Matches {@code pattern} against the URI path (or full URI) and extracts any named
     * regex capturing groups (e.g., {@code (?<id>[^/]+)}) into an unmodifiable map.
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

    public Map<String, String> extractPathParams(String regex) {
        return (regex != null) ? extractPathParams(Pattern.compile(regex)) : Collections.emptyMap();
    }

    @Override
    public String toString() {
        return (uri != null) ? uri.toString() : null;
    }
}
