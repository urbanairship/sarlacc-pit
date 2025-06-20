package com.urbanairship.sarlacc.client.structures.container;

import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Service;

public abstract class UpdatingCollection<C> {
    private Service updateService = null;
    private boolean blockReads = false;

    public synchronized void setUpdateService(Service updateService) {
        if (this.updateService != null) {
            throw new IllegalStateException("UpdateService already set!");
        }

        this.updateService = Preconditions.checkNotNull(updateService);
    }

    /**
     * `this` should always be an instance of C, but constructs that would allow us to guarantee that to the type checker
     * end up being uglier than the below. The user of this library should never need to know this exists unless they're
     * implementing a new collection wrapper. In any case, if the cast fails, the service build() will fail, which is
     * probably enough to prevent misuse.
     */
    @SuppressWarnings("unchecked")
    public C getAsCollectionType() {
        return (C) this;
    }

    public void setBlockReads(boolean blockReads) {
        this.blockReads = blockReads;
    }

    protected void checkState() {
        if (updateService == null) {
            throw new IllegalStateException("Backing update service was never set!");
        } else if (blockReads) {
            throw new IllegalStateException("Data too stale, read failed!");
        } else if (updateService.state() != Service.State.RUNNING) {
            final String msg = String.format(
                    "Attempted to read updating collection backed by non-running update service. State: '%s'",
                    updateService.state());
            throw new IllegalStateException(msg);
        }
    }
}
