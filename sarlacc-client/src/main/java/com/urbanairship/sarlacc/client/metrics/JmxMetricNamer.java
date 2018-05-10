package com.urbanairship.sarlacc.client.metrics;

import java.util.Optional;

public class JmxMetricNamer implements MetricNamer {
    @Override
    public String name(Class clazz, String name, Optional<String> scope) {
        final String group = clazz.getPackage() == null ? "" : clazz.getPackage().getName();
        final String type = clazz.getSimpleName().replaceAll("\\$$", "");

        return new StringBuilder()
                .append(group)
                .append(":type=").append(type)
                .append(scope.map(s -> ",scope=" + s).orElse(""))
                .append(",name=").append(name)
                .toString();
    }
}
