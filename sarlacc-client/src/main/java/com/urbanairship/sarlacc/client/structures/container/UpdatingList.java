package com.urbanairship.sarlacc.client.structures.container;

import com.google.common.collect.Iterators;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;

public class UpdatingList<T> extends UpdatingCollection<List<T>> implements List<T> {

    private final AtomicReference<List<T>> delegate;

    public UpdatingList(AtomicReference<List<T>> delegate) {
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
    public T get(int i) {
        checkState();
        return delegate.get().get(i);
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
    public boolean addAll(int i, Collection<? extends T> ts) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> objects) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T set(int i, T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int i, T t) {
        throw new UnsupportedOperationException();
    }

    @Override
    public T remove(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ListIterator<T> listIterator(int i) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<T> subList(int i, int i2) {
        throw new UnsupportedOperationException();
    }
}
