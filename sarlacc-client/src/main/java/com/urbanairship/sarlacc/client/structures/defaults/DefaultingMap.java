package com.urbanairship.sarlacc.client.structures.defaults;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A map that returns a default value for all get() calls. If instead a map that returns nothing is desired,
 * use ImmutableMap.of();
 */
public class DefaultingMap<K, V> implements Map<K, V> {
    private final V valueToReturn;

    public DefaultingMap(V valueToReturn) {
        this.valueToReturn = valueToReturn;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return true;
    }

    @Override
    public boolean containsValue(Object other) {
        return valueToReturn.equals(other);
    }

    @Override
    public V get(Object key) {
        return valueToReturn;
    }

    @Override
    public V put(K key, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object key) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<K> keySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<V> values() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefaultingMap)) return false;

        DefaultingMap<?, ?> that = (DefaultingMap<?, ?>) o;

        return valueToReturn != null ? valueToReturn.equals(that.valueToReturn) : that.valueToReturn == null;
    }

    @Override
    public int hashCode() {
        return valueToReturn != null ? valueToReturn.hashCode() : 0;
    }
}
