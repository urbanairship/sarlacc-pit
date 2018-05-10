package com.urbanairship.sarlacc.client.structures.container;

import com.google.common.collect.Iterators;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class UpdatingSet<T> extends UpdatingCollection<Set<T>> implements Set<T> {

    private final AtomicReference<Set<T>> delegate;

    public UpdatingSet(AtomicReference<Set<T>> delegate) {
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
    public boolean contains(Object o) {
        checkState();
        return delegate.get().contains(o);
    }

    @Override
    public Iterator<T> iterator() {
        checkState();
        return Iterators.unmodifiableIterator(delegate.get().iterator());
    }

    @Override
    public Object[] toArray() {
        checkState();
        return delegate.get().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] t1s) {
        checkState();
        return delegate.get().toArray(t1s);
    }

    @Override
    public boolean containsAll(Collection<?> objects) {
        checkState();
        return delegate.get().containsAll(objects);
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
    public boolean add(T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(Collection<? extends T> ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
