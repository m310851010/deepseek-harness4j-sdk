package com.deepseek.harness4j.util.flow;

/**
 * JDK 8 backport of {@link java.util.concurrent.Flow.Publisher} (Java 9+).
 *
 * <p>A producer of items received by a {@link Subscriber}.
 */
public interface Publisher<T> {

    void subscribe(Subscriber<? super T> subscriber);
}
