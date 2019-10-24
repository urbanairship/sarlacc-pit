package com.urbanairship.sarlacc.client;

import com.urbanairship.sarlacc.client.model.LastSuccessDetails;

import java.util.Optional;

public interface FailureCallback {
    enum Action {
        NO_ACTION,
        BLOCK_READS,
        SHUT_DOWN
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
     *                           missing if and only if the initial fetch and process fails.
     * @param thrown The exception or error which caused the fetch call to fail
     *
     * @return the action for the update service to take until the next successful check
     */
    Action onFailure(Optional<LastSuccessDetails> lastSuccessDetails, Throwable thrown);
}
