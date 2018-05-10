package com.urbanairship.sarlacc.client;

public interface FetchFailureCallback {

    /**
     * Function to be called when a fetchIfNewer fails. fetchIfNewer returning no
     * update is not considered a failure. If the initial fetch fails, this function
     * will not be called, but the service will fail to start.
     *
     * If an implementation on onFetchFailure itself throws an exception, it will be
     * logged and otherwise ignored.
     *
     * @param lastKnownVersion Version of the last fetch that returned an update
     * @param lastSuccessfulCheck Epoch time of the last fetch call that didn't fail
     * @param failingChecksSinceLastSuccess How many checks in succession have failed. Will be 1 on first failure
     *                                      2 on second, and so on.
     * @param thrown The exception or error which caused the fetch call to fail
     */
    void onFetchFailure(long lastKnownVersion, long lastSuccessfulCheck, int failingChecksSinceLastSuccess, Throwable thrown);
}
