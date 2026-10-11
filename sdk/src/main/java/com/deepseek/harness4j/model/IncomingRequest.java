package com.deepseek.harness4j.model;

import java.util.Map;
import java.util.Objects;

/**
 * An incoming JSON-RPC request from the runtime (a request the client must answer).
 *
 * <p>Python (dataclass with {@code slots=True}):
 * <pre>{@code
 * @dataclass(slots=True)
 * class IncomingRequest:
 *     id: str | int
 *     method: str
 *     payload: JsonObject
 * }</pre>
 *
 * <p>The {@code id} is a JSON-RPC request id: a string or an integer, hence
 * {@link Object}. Use {@link #idAsString()} to normalize for map lookups.
 */
public final class IncomingRequest {

    private final Object id;
    private final String method;
    private final Map<String, Object> payload;

    public IncomingRequest(Object id, String method, Map<String, Object> payload) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(payload, "payload");
        this.id = id;
        this.method = method;
        this.payload = payload;
    }

    /** The JSON-RPC request id (string or integer). */
    public Object id() {
        return id;
    }

    /** The request method name. */
    public String method() {
        return method;
    }

    /** The request payload. */
    public Map<String, Object> payload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof IncomingRequest)) {
            return false;
        }
        IncomingRequest that = (IncomingRequest) o;
        return Objects.equals(id, that.id)
                && Objects.equals(method, that.method)
                && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, method, payload);
    }

    @Override
    public String toString() {
        return "IncomingRequest[id=" + id + ", method=" + method + ", payload=" + payload + ']';
    }

    /**
     * @return the request id rendered as a string, matching how the Python
     *         client normalizes ids when indexing its response waiters.
     */
    public String idAsString() {
        return String.valueOf(id);
    }
}
