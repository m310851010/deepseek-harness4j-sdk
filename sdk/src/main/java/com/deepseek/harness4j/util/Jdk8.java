package com.deepseek.harness4j.util;

import com.deepseek.harness4j.util.Jdk8;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JDK 8 backports of the Java 9+ collection factories and small helpers used by the SDK.
 *
 * <p>Semantics mirror the JDK factory methods as closely as Java 8 allows: the returned
 * collections are immutable and reject null elements/keys/values. Iteration order of
 * {@link #setOf} and {@link #mapOf} follows insertion order (the JDK variants make no
 * ordering guarantee; preserving it is strictly friendlier to callers and tests).
 */
public final class Jdk8 {

    private Jdk8() {
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... elements) {
        for (T element : elements) {
            if (element == null) {
                throw new NullPointerException("listOf rejects null elements");
            }
        }
        switch (elements.length) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(elements[0]);
            default:
                return Collections.unmodifiableList(new ArrayList<>(Arrays.asList(elements)));
        }
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements) {
        Set<T> set = new LinkedHashSet<>(elements.length * 2 + 1);
        for (T element : elements) {
            if (element == null) {
                throw new NullPointerException("setOf rejects null elements");
            }
            if (!set.add(element)) {
                throw new IllegalArgumentException("duplicate element: " + element);
            }
        }
        return Collections.unmodifiableSet(set);
    }

    public static <K, V> Map<K, V> mapOf() {
        return Collections.emptyMap();
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1) {
        return pairs(1, new Object[]{k1, v1});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2) {
        return pairs(2, new Object[]{k1, v1, k2, v2});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        return pairs(3, new Object[]{k1, v1, k2, v2, k3, v3});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return pairs(4, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return pairs(5, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4, k5, v5});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6) {
        return pairs(6, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7) {
        return pairs(7, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8) {
        return pairs(8, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {
        return pairs(9, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9});
    }

    public static <K, V> Map<K, V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5,
                                         K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {
        return pairs(10, new Object[]{k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8,
                k9, v9, k10, v10});
    }

    private static <K, V> Map<K, V> pairs(int expected, Object[] flat) {
        Map<K, V> map = new LinkedHashMap<>(expected * 2 + 1);
        for (int i = 0; i < flat.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) flat[i];
            @SuppressWarnings("unchecked")
            V value = (V) flat[i + 1];
            if (key == null || value == null) {
                throw new NullPointerException("mapOf rejects null keys and values");
            }
            V previous = map.put(key, value);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate key: " + key);
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /** Backport of {@code List.copyOf}/{@code Set.copyOf}/{@code Map.copyOf} for JDK 8. */
    public static <T> List<T> copyOf(Collection<? extends T> collection) {
        List<T> list = new ArrayList<>(collection.size());
        for (T element : collection) {
            if (element == null) {
                throw new NullPointerException("copyOf rejects null elements");
            }
            list.add(element);
        }
        return Collections.unmodifiableList(list);
    }

    /** Backport of {@code InputStream.readAllBytes()} (Java 9+) for JDK 8. */
    public static byte[] readAllBytes(InputStream in) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream(8192);
        byte[] chunk = new byte[8192];
        int read;
        while ((read = in.read(chunk)) != -1) {
            buffer.write(chunk, 0, read);
        }
        return buffer.toByteArray();
    }

    /** Backport of {@code Jdk8.isBlank(String)} (Java 11+) for JDK 8. */
    public static boolean isBlank(CharSequence value) {
        if (value == null) {
            throw new NullPointerException();
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isWhitespace(value.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /** Backport of {@code Files.readString(Path)} (Java 11+, UTF-8) for JDK 8. */
    public static String readString(java.nio.file.Path path) throws IOException {
        return new String(java.nio.file.Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Backport of {@code Files.readString(Path, Charset)} (Java 11+) for JDK 8. */
    public static String readString(java.nio.file.Path path, java.nio.charset.Charset charset)
            throws IOException {
        return new String(java.nio.file.Files.readAllBytes(path), charset);
    }

    /** Backport of {@code Jdk8.writeString(Path, CharSequence, Charset)} (Java 11+) for JDK 8. */
    public static void writeString(java.nio.file.Path path, CharSequence content,
                                   java.nio.charset.Charset charset) throws IOException {
        java.nio.file.Files.write(path, content.toString().getBytes(charset));
    }

    /** Backport of {@code Files.writeString(Path, CharSequence)} (Java 11+, UTF-8) for JDK 8. */
    public static void writeString(java.nio.file.Path path, CharSequence content) throws IOException {
        writeString(path, content, java.nio.charset.StandardCharsets.UTF_8);
    }

    /** Backport of {@code Files.writeString(Path, CharSequence, OpenOption...)} (Java 11+) for JDK 8. */
    public static void writeString(java.nio.file.Path path, CharSequence content,
                                   java.nio.file.OpenOption... options) throws IOException {
        writeString(path, content, java.nio.charset.StandardCharsets.UTF_8, options);
    }

    /** Backport of {@code Files.writeString(Path, CharSequence, Charset, OpenOption...)} (Java 11+) for JDK 8. */
    public static void writeString(java.nio.file.Path path, CharSequence content,
                                   java.nio.charset.Charset charset,
                                   java.nio.file.OpenOption... options) throws IOException {
        java.nio.file.Files.write(path, content.toString().getBytes(charset), options);
    }
}
