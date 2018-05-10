package com.urbanairship.sarlacc.client.processor;

import java.io.IOException;

public interface UpdateProcessor<S, C> {
    C process(S input) throws IOException;
}
