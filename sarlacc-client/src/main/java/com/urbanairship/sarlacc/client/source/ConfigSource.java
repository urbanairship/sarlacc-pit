package com.urbanairship.sarlacc.client.source;

import com.urbanairship.sarlacc.client.model.Update;

import java.io.IOException;
import java.util.Optional;

public interface ConfigSource<T> {
    Update<T> fetch() throws IOException;

    default Optional<Update<T>> fetchIfNewer(long ifNewerThan) throws IOException {
        return Optional.of(fetch());
    }
}
