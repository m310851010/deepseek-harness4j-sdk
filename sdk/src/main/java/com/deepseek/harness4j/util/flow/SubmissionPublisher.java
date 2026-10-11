package com.deepseek.harness4j.util.flow;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * JDK 8 stand-in for {@link java.util.concurrent.SubmissionPublisher} (Java 9+).
 *
 * <p>Covers the subset of behavior the SDK relies on: a single subscriber, buffered
 * {@link #submit(Object)} when demand is outstanding-low, {@link #close()} /
 * {@link #closeExceptionally(Throwable)} termination, and {@link #isClosed()} polling.
 * Delivery happens synchronously on the submitting thread (or the requesting thread),
 * which is what the SDK's streaming call sites do.
 */
public final class SubmissionPublisher<T> implements Publisher<T>, AutoCloseable {

    private final Object lock = new Object();

    private Subscriber<? super T> subscriber;
    private Sub subscription;
    private final Deque<T> buffer = new ArrayDeque<>();
    private long demand = 0;
    private boolean closed = false;
    private Throwable error = null;
    private boolean errorDelivered = false;

    @Override
    public void subscribe(Subscriber<? super T> subscriber) {
        synchronized (lock) {
            if (this.subscriber != null) {
                throw new IllegalStateException("SubmissionPublisher supports only one subscriber");
            }
            if (subscriber == null) {
                throw new NullPointerException();
            }
            this.subscriber = subscriber;
            this.subscription = new Sub();
        }
        subscriber.onSubscribe(subscription);
        drain();
    }

    /** Publish one item; buffered while the subscriber has not requested it. */
    public void submit(T item) {
        synchronized (lock) {
            if (closed) {
                throw new IllegalStateException("publisher is closed");
            }
            if (item == null) {
                throw new NullPointerException();
            }
            buffer.add(item);
        }
        drain();
    }

    /** Signal normal completion; buffered items are still delivered first. */
    public void close() {
        synchronized (lock) {
            closed = true;
        }
        drain();
    }

    /** Signal failure; the error is delivered to the subscriber (once). */
    public void closeExceptionally(Throwable throwable) {
        synchronized (lock) {
            closed = true;
            error = throwable;
        }
        drain();
    }

    public boolean isClosed() {
        synchronized (lock) {
            return closed;
        }
    }

    /** Number of items buffered but not yet requested (approximate, like the JDK variant). */
    public int estimateMaximumLag() {
        synchronized (lock) {
            return buffer.size();
        }
    }

    private void drain() {
        Subscriber<? super T> current;
        synchronized (lock) {
            current = subscriber;
            if (current == null) {
                return;
            }
        }
        while (true) {
            T item = null;
            boolean completeNow = false;
            boolean failNow = false;
            Throwable pendingError = null;
            synchronized (lock) {
                if (error != null && !errorDelivered) {
                    errorDelivered = true;
                    pendingError = error;
                    failNow = true;
                } else if (!buffer.isEmpty() && demand > 0) {
                    demand--;
                    item = buffer.poll();
                } else if (buffer.isEmpty() && closed && error == null) {
                    completeNow = true;
                }
            }
            try {
                if (failNow) {
                    current.onError(pendingError);
                    return;
                }
                if (item != null) {
                    current.onNext(item);
                } else if (completeNow) {
                    current.onComplete();
                    return;
                } else {
                    return;
                }
            } catch (Throwable thrown) {
                // an onNext that throws cancels the subscription, like SubmissionPublisher
                synchronized (lock) {
                    closed = true;
                    buffer.clear();
                    subscriber = null;
                }
                return;
            }
        }
    }

    private final class Sub implements Subscription {

        @Override
        public void request(long numberOfItems) {
            if (numberOfItems <= 0) {
                throw new IllegalArgumentException("non-positive request");
            }
            synchronized (lock) {
                long next = demand + numberOfItems;
                demand = next < 0 ? Long.MAX_VALUE : next; // saturate on overflow
            }
            drain();
        }

        @Override
        public void cancel() {
            synchronized (lock) {
                closed = true;
                buffer.clear();
                subscriber = null;
            }
        }
    }
}
