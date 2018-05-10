package com.urbanairship.sarlacc.client.structures.impl;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import java.util.*;

/**
 * A set implementation backed by an array, using binary search for contains() queries.
 *
 * Benchmarking has shown this to be about 3x slower than a guava ImmutableSet, but its
 * memory footprint is much smaller. Lookup times were ~0.5ns with 310k items.
 */
public class ImmutableArrayStringSet implements Set<String> {
    private final String[] backing;
    private final int size;

    /*
     * Benchmarking showed 33% speed improvements when comparing first by hash code
     * then normal lex compare, largely because String instances cache their hash.
     */
    private static final Comparator<String> HASH_COMPARATOR = (o1, o2) -> {
        int h1 = o1.hashCode();
        int h2 = o2.hashCode();

        if (h1 == h2) {
            return o1.compareTo(o2);
        } else {
            return h1 < h2 ? -1 : 1;
        }
    };

    private ImmutableArrayStringSet(String[] backing, int size) {
        this.backing = backing;
        this.size = size;
    }

    /*
     * Copied with minimal changes from Arrays.binarySearch()
     */
    @Override
    public boolean contains(Object key) {
        if (!(key instanceof String)) {
            return false;
        }

        final String keyStr = (String) key;
        int low = 0;
        int high = size - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int cmp = HASH_COMPARATOR.compare(backing[mid], keyStr);

            if (cmp < 0)
                low = mid + 1;
            else if (cmp > 0)
                high = mid - 1;
            else
                return true;
        }
        return false;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public Iterator<String> iterator() {
        return Iterables.unmodifiableIterable(Lists.newArrayList(backing)).iterator();
    }

    @Override
    public Object[] toArray() {
        return Arrays.copyOf(backing, size);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T[] toArray(T[] a) {
        return (T[]) Arrays.copyOf(backing, size);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return Iterables.all(c, (Predicate<Object>) this::contains);
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static class Builder {
        final TreeSet<String> treeSet;

        private Builder() {
            treeSet = new TreeSet<>(HASH_COMPARATOR);
        }

        public Builder add(String element) {
            Preconditions.checkNotNull(element);
            treeSet.add(element);

            return this;
        }

        public ImmutableArrayStringSet build() {
            String[] buffer = treeSet.toArray(new String[treeSet.size()]);
            return new ImmutableArrayStringSet(buffer, buffer.length);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!obj.getClass().isInstance(Set.class)) {
            return false;
        }

        Set<String> otherSet = (Set<String>) obj;

        if (this.size() != otherSet.size()) {
            return false;
        }

        for (String s : otherSet) {
            if (!this.contains(s)) {
                return false;
            }
        }

        return true;
    }

    @Override
    public boolean remove(Object o) {
        throw new java.lang.UnsupportedOperationException("This set is immutable.");
    }

    @Override
    public boolean add(String s) {
        throw new UnsupportedOperationException("This set is immutable.");
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        throw new UnsupportedOperationException("This set is immutable.");
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("This set is immutable.");
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("This set is immutable.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("This set is immutable.");
    }

    @Override
    public String toString() {
        return Arrays.toString(backing);
    }
}
