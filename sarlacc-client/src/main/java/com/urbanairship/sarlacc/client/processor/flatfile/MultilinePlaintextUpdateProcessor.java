package com.urbanairship.sarlacc.client.processor.flatfile;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.CharStreams;
import com.urbanairship.sarlacc.client.processor.UpdateProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class MultilinePlaintextUpdateProcessor<C> implements UpdateProcessor<InputStream, C> {
    private final Supplier<? extends AbstractLineProcessor<C>> processorSupplier;

    public MultilinePlaintextUpdateProcessor(Supplier<? extends AbstractLineProcessor<C>> processorSupplier) {
        this.processorSupplier = processorSupplier;
    }

    @Override
    public C process(InputStream input) throws IOException {
        Preconditions.checkNotNull(input);

        AbstractLineProcessor<C> lineProcessor = processorSupplier.get();
        final InputStreamReader readable = new InputStreamReader(input);

        CharStreams.readLines(readable, lineProcessor);
        return lineProcessor.getResult();
    }

    public static <T> MultilinePlaintextUpdateProcessor<List<T>> list(final Function<String, T> parseFunc) {
        final Supplier<AbstractLineProcessor<List<T>>> supplier = () -> new AbstractLineProcessor<List<T>>() {
            private final ImmutableList.Builder<T> builder = ImmutableList.builder();

            @Override
            public void process(String line) {
                T item = Preconditions.checkNotNull(parseFunc.apply(line));
                builder.add(item);
            }

            @Override
            public List<T> getResult() {
                return builder.build();
            }
        };

        return new MultilinePlaintextUpdateProcessor<>(supplier);
    }

    public static <T> MultilinePlaintextUpdateProcessor<Set<T>> set(final Function<String, T> parseFunc) {
        final Supplier<AbstractLineProcessor<Set<T>>> supplier = () -> new AbstractLineProcessor<Set<T>>() {
            private final ImmutableSet.Builder<T> builder = ImmutableSet.builder();

            @Override
            public void process(String line) {
                T item = Preconditions.checkNotNull(parseFunc.apply(line));
                builder.add(item);
            }

            @Override
            public Set<T> getResult() {
                return builder.build();
            }
        };

        return new MultilinePlaintextUpdateProcessor<>(supplier);
    }

    public static <K, V> MultilinePlaintextUpdateProcessor<Map<K, V>> map(final Function<String, Map.Entry<K, V>> parseFunc) {
        final Supplier<AbstractLineProcessor<Map<K, V>>> supplier = () -> new AbstractLineProcessor<Map<K, V>>() {
            private final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

            @Override
            public void process(String line) {
                Map.Entry<K, V> entry = Preconditions.checkNotNull(parseFunc.apply(line));
                builder.put(entry);
            }

            @Override
            public Map<K, V> getResult() {
                return builder.build();
            }
        };

        return new MultilinePlaintextUpdateProcessor<>(supplier);
    }
}
