package com.deepseek.harness4j.util.flow;

/**
 * JDK 8 backport of {@link java.util.concurrent.Flow.Subscription} (Java 9+).
 */
public interface Subscription {

    void request(long numberOfItems);

    void cancel();
}
