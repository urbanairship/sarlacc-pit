package com.urbanairship.sarlacc.client.model;

import java.io.Closeable;

public class Update<T> implements AutoCloseable {
    public final long version;
    public final T newVal;

    public Update(long version, T newVal) {
        this.version = version;
        this.newVal = newVal;
    }

    @Override
    public void close() throws Exception {
        if (newVal instanceof Closeable closeable1) {
            closeable1.close();
        } else if (newVal instanceof AutoCloseable closeable) {
            closeable.close();
        }
    }
}
