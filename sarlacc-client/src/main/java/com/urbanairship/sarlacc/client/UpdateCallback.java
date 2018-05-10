package com.urbanairship.sarlacc.client;

import java.util.Optional;

/**
 * @param <T> The collection type being updated
 */
public interface UpdateCallback<T> {

    /**
     * Function to be called whenever fetchIfNewer returns an update. Implementers should keep in
     * mind that no guarantee is made that previous and current are actually different, as this
     * depends on the semantics of the ConfigSource being used and possibly on clocks being synchronized.
     * While the new collection value will be exposed before this is called, implementors should be careful
     * to not make implementations too computationally intensive or blocking, as no new fetch calls will be
     * made until onUpdate finishes. It's guaranteed that calls from a single sarlacc-pit service will never
     * overlap.
     *
     * On first successful fetch this function will be called with the previous value and version Optional.empty()
     *
     * If a call to onUpdate throws an exception it is logged and the 'Update Callback Errors' metric
     * is incremented. Implementations are responsible for any additional handling that may be necessary.
     *
     * @param previous The collection being replaced, if any
     * @param previousVersion The source reported version of the previous value of the collection, if any
     * @param current The new collection value.
     * @param currentVersion The source reported version of the new collection.
     */
    void onUpdate(Optional<T> previous, Optional<Long> previousVersion, T current, long currentVersion);
}
