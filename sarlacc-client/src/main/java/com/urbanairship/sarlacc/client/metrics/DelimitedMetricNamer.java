package com.urbanairship.sarlacc.client.metrics;

import com.google.common.base.Preconditions;
import org.apache.commons.lang.StringUtils;

import java.util.Optional;

public class DelimitedMetricNamer implements MetricNamer {
    private static final String REGEX_SPECIAL = "<([{\\^-=$!|]})?*+.>";

    private final String delimiter;
    private final String cleanRegex;
    private final String replacement;

    public DelimitedMetricNamer(String delimiter) {
        this(delimiter, defaultCleanRegex(delimiter), "");
    }

    public DelimitedMetricNamer(String delimiter, String cleanRegex, String replacement) {
        this.delimiter = Preconditions.checkNotNull(delimiter, "Delimiter must not be null");
        this.cleanRegex = Preconditions.checkNotNull(cleanRegex, "Clean Regex must not be null");
        this.replacement = Preconditions.checkNotNull(replacement, "Replacement must not be null");
    }

    private static String defaultCleanRegex(String delimiter) {
        final StringBuilder escapedDelimiter = new StringBuilder();
        for (char c : delimiter.toCharArray()) {
            if (StringUtils.contains(REGEX_SPECIAL, c)) {
                escapedDelimiter.append("\\");
            }
            escapedDelimiter.append(c);
        }
        return "(\\$$)|(%s)".formatted(escapedDelimiter);
    }

    @Override
    public String name(Class clazz, String name, Optional<String> scope) {
        final String cleanClazz = clazz.getCanonicalName().replaceAll(cleanRegex, replacement);
        final String cleanName = name.replaceAll(cleanRegex, replacement);
        final Optional<String> cleanScope = scope.map(s -> s.replaceAll(cleanRegex, replacement));

        if (cleanScope.isPresent()) {
            return String.join(delimiter, cleanClazz, cleanName, cleanScope.get());
        } else {
            return String.join(delimiter, cleanClazz, cleanName);
        }
    }
}
