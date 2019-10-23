package com.urbanairship.sarlacc.client;

import java.time.Instant;
import java.util.Optional;

public interface FailureCallback {
    enum Action {
        NO_ACTION,
        BLOCK_READS,
        SHUT_DOWN
    }

    class LastSuccessDetails {
        // Version of the last fetch that returned an update
        public final long lastKnownVersion;

        // Time of the last fetch call that didn't fail
        public final Instant lastSuccessfulCheck;

        // How many checks in succession have failed. Will be 1 on first failure 2 on second, and so on.
        public final int failuresSinceLastSuccess;

        public LastSuccessDetails(long lastKnownVersion, Instant lastSuccessfulCheck, int failuresSinceLastSuccess) {
            this.lastKnownVersion = lastKnownVersion;
            this.lastSuccessfulCheck = lastSuccessfulCheck;
            this.failuresSinceLastSuccess = failuresSinceLastSuccess;
        }
    }

    /**
     * Function to be called when a fetchIfNewer or update process fails. fetchIfNewer returning no
     * update is not considered a failure. If the initial fetch fails, this function
     * will not be called, but the service will fail to start.
     *
     * If an implementation of onFailure itself throws an exception, it will be
     * logged and otherwise ignored.
     *
     * @param lastSuccessDetails If a previous call succeeded this will contain information about it. This will be
     *                           missing only if the initial fetch and process fails.
     * @param thrown The exception or error which caused the fetch call to fail
     *
     * @return the action for the update service to take until the next successful check
     */
    Action onFailure(Optional<LastSuccessDetails> lastSuccessDetails, Throwable thrown);
}
