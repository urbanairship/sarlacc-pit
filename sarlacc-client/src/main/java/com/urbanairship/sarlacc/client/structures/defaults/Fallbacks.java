package com.urbanairship.sarlacc.client.structures.defaults;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;
import java.util.Set;

/**
 * Provides a set of collections for use as Fallback values
 */
public class Fallbacks {
    private Fallbacks() {
    }

    public static <O> Set<O> containsAllSet() {
        return new ContainsAllSet<>();
    }

    public static <O> Set<O> containsNoneSet() {
        return ImmutableSet.of();
    }

    public static <K, V> Map<K, V> mapFromAllKeysToSingleValue(V value) {
        return new DefaultingMap<>(value);
    }

    public static <K, V> Map<K, V> emptyMap(V value) {
        return ImmutableMap.of();
    }
}
