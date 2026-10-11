package com.deepseek.harness4j.model;

import java.util.Map;
import java.util.Objects;

/**
 * A JSON-RPC notification received from the runtime.
 *
 * <p>Python (dataclass with {@code slots=True}):
 * <pre>{@code
 * @dataclass(slots=True)
 * class Notification:
 *     method: str
 *     payload: JsonObject
 * }</pre>
 */
public final class Notification {

    private final String method;
    private final Map<String, Object> payload;

    public Notification(String method, Map<String, Object> payload) {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(payload, "payload");
        this.method = method;
        this.payload = payload;
    }

    /** The notification method name, e.g. {@code session.event}. */
    public String method() {
        return method;
    }

    /** The notification payload. */
    public Map<String, Object> payload() {
        return payload;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Notification)) {
            return false;
        }
        Notification that = (Notification) o;
        return Objects.equals(method, that.method)
                && Objects.equals(payload, that.payload);
    }

    @Override
    public int hashCode() {
        return Objects.hash(method, payload);
    }

    @Override
    public String toString() {
        return "Notification[method=" + method + ", payload=" + payload + ']';
    }
}
