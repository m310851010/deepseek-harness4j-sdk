package com.deepseek.harness4j.util.flow;

/**
 * JDK 8 backport of {@link java.util.concurrent.Flow.Subscriber} (Java 9+).
 */
public interface Subscriber<T> {

    void onSubscribe(Subscription subscription);

    void onNext(T item);

    void onError(Throwable throwable);

    void onComplete();
}
