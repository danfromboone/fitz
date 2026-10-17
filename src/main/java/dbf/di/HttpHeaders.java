package dbf.di;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class HttpHeaders {
    private final Map<String, List<String>> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public HttpHeaders() {
    }

    public HttpHeaders(Map<String, List<String>> initialHeaders) {
        if (initialHeaders != null) {
            for (Map.Entry<String, List<String>> entry : initialHeaders.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null) {
                    headers.put(entry.getKey(), new ArrayList<>(entry.getValue()));
                }
            }
        }
    }

    public HttpHeaders(HttpServletRequest request) {
        if (request != null) {
            Enumeration<String> headerNames = request.getHeaderNames();
            if (headerNames != null) {
                while (headerNames.hasMoreElements()) {
                    String name = headerNames.nextElement();
                    Enumeration<String> values = request.getHeaders(name);
                    List<String> valueList = (values != null)
                            ? new ArrayList<>(Collections.list(values))
                            : new ArrayList<>();
                    headers.put(name, valueList);
                }
            }
        }
    }

    public static HttpHeaders from(HttpServletRequest request) {
        return new HttpHeaders(request);
    }

    public Map<String, List<String>> asMap() {
        Map<String, List<String>> view = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            view.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(view);
    }

    public Set<String> getNames() {
        return Collections.unmodifiableSet(headers.keySet());
    }

    public List<String> getValues(String name) {
        if (name == null) {
            return Collections.emptyList();
        }
        List<String> values = headers.get(name);
        return (values != null) ? Collections.unmodifiableList(values) : Collections.emptyList();
    }

    public String getFirst(String name) {
        if (name == null) {
            return null;
        }
        List<String> values = headers.get(name);
        return (values == null || values.isEmpty()) ? null : values.getFirst();
    }

    public void set(String name, String value) {
        if (name != null) {
            List<String> values = new ArrayList<>();
            if (value != null) {
                values.add(value);
            }
            headers.put(name, values);
        }
    }

    public void add(String name, String value) {
        if (name != null && value != null) {
            headers.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
    }

    public boolean contains(String name) {
        return name != null && headers.containsKey(name);
    }

    public boolean isEmpty() {
        return headers.isEmpty();
    }

    @Override
    public String toString() {
        return headers.toString();
    }
}
