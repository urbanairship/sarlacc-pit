package com.urbanairship.sarlacc.client.processor.flatfile;

import com.urbanairship.sarlacc.client.structures.impl.ImmutableArrayStringSet;
import org.apache.commons.lang.StringUtils;

import java.util.Set;
import java.util.function.Supplier;

public final class ImmutableArrayStringSetLineProcessor extends AbstractLineProcessor<Set<String>> {
    public static final Supplier<ImmutableArrayStringSetLineProcessor> SUPPLIER = ImmutableArrayStringSetLineProcessor::new;

    private final ImmutableArrayStringSet.Builder builder;
    private ImmutableArrayStringSet arrayStringSet;

    public ImmutableArrayStringSetLineProcessor() {
        builder = ImmutableArrayStringSet.newBuilder();
    }

    @Override
    public void process(String line) {
        if (StringUtils.isNotBlank(line)) {
            builder.add(line);
        }
    }

    @Override
    public ImmutableArrayStringSet getResult() {
        if (arrayStringSet == null) {
            arrayStringSet = builder.build();
        }

        return arrayStringSet;
    }
}
