package com.urbanairship.sarlacc.client.model;

import java.time.Instant;
import java.util.Objects;

public class LastSuccessDetails {
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

    @Override
    public String toString() {
        return "LastSuccessDetails{" +
                "lastKnownVersion=" + lastKnownVersion +
                ", lastSuccessfulCheck=" + lastSuccessfulCheck +
                ", failuresSinceLastSuccess=" + failuresSinceLastSuccess +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LastSuccessDetails)) return false;
        LastSuccessDetails that = (LastSuccessDetails) o;
        return lastKnownVersion == that.lastKnownVersion &&
                failuresSinceLastSuccess == that.failuresSinceLastSuccess &&
                Objects.equals(lastSuccessfulCheck, that.lastSuccessfulCheck);
    }

    @Override
    public int hashCode() {
        return Objects.hash(lastKnownVersion, lastSuccessfulCheck, failuresSinceLastSuccess);
    }
}
