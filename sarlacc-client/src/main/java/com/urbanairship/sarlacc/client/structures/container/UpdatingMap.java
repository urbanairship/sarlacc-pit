package com.urbanairship.sarlacc.client.structures.container;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdatingMap<K,V> extends UpdatingCollection<Map<K, V>> implements Map<K, V> {

    private final AtomicReference<Map<K,V>> delegate;

    public UpdatingMap(AtomicReference<Map<K, V>> delegate) {
        this.delegate = delegate;
    }

    @Override
    public int size() {
        checkState();
        return delegate.get().size();
    }

    @Override
    public boolean isEmpty() {
        checkState();
        return this.size() == 0;
    }

    @Override
    public boolean containsKey(Object o) {
        checkState();
        return delegate.get().containsKey(o);
    }

    @Override
    public boolean containsValue(Object o) {
        checkState();
        return delegate.get().containsValue(o);
    }

    @Override
    public V get(Object o) {
        checkState();
        return delegate.get().get(o);
    }

    @Override
    public Set<K> keySet() {
        checkState();
        return delegate.get().keySet();
    }

    @Override
    public Collection<V> values() {
        checkState();
        return delegate.get().values();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        checkState();
        return delegate.get().entrySet();
    }

    @Override
    public boolean equals(Object o) {
        checkState();
        return delegate.get().equals(o);
    }

    @Override
    public int hashCode() {
        checkState();
        return delegate.get().hashCode();
    }

    @Override
    public String toString() {
        checkState();
        return delegate.get().toString();
    }

    @Override
    public V put(K k, V v) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> map) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
