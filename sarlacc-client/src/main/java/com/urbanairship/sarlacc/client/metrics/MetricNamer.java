package com.urbanairship.sarlacc.client.metrics;

import java.util.Optional;

public interface MetricNamer {
    String name(Class clazz, String name, Optional<String> scope);
}
