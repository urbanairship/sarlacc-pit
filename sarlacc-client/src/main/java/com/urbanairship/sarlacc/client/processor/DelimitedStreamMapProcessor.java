package com.urbanairship.sarlacc.client.processor;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

public class DelimitedStreamMapProcessor<K, V> implements UpdateProcessor<InputStream, Map<K, V>>{
    private final LineProcessor<Map<K, V>> lineProcessor;

    private DelimitedStreamMapProcessor(LineProcessor<Map<K, V>> lineProcessor) {
        this.lineProcessor = lineProcessor;
    }

    @Override
    public Map<K, V> process(InputStream input) throws IOException {
        return CharStreams.readLines(new InputStreamReader(input), lineProcessor);
    }

    public static SupplierBuilder<String, String> supplierBuilder() {
        return new SupplierBuilder<>();
    }

    @SuppressWarnings("unchecked")
    public static class SupplierBuilder<K, V> {
        private Function keyParser = Function.identity();
        private Function valueParser = Function.identity();

        private String delimiter;

        public <NK> SupplierBuilder<NK, V> setKeyParser(Function<String, NK> keyParser) {
            this.keyParser = keyParser;
            return (SupplierBuilder<NK, V>) this;
        }

        public <NV> SupplierBuilder<K, NV> setValueParser(Function<String, NV> valueParser) {
            this.valueParser = valueParser;
            return (SupplierBuilder<K, NV>) this;
        }

        public SupplierBuilder<K, V> setDelimiter(String delimiter) {
            this.delimiter = delimiter;
            return this;
        }

        public Supplier<DelimitedStreamMapProcessor<K, V>> build() {
            return () -> new DelimitedStreamMapProcessor<>(new ParsingLineProcessor<>(keyParser, valueParser, delimiter));
        }
    }

    private static class ParsingLineProcessor<K, V> implements LineProcessor<Map<K, V>> {
        private final ImmutableMap.Builder<K, V> mapBuilder = ImmutableMap.builder();

        private final Function<String, K> keyParser;
        private final Function<String, V> valueParser;
        private final String delimiter;

        public ParsingLineProcessor(Function<String, K> keyParser, Function<String, V> valueParser, String delimiter) {
            this.keyParser = keyParser;
            this.valueParser = valueParser;
            this.delimiter = delimiter;
        }

        @Override
        public boolean processLine(String line) throws IOException {
            final String[] split = line.split(delimiter);
            if (split.length != 2) {
                throw new RuntimeException(String.format("Expected line in format 'key%svalue', but got: '%s'", delimiter, line));
            }

            final K key;
            try {
                key = keyParser.apply(split[0]);
            } catch (Throwable t) {
                throw new RuntimeException(String.format("Couldn't parse key from line: '%s'", line), t);
            }

            final V value;
            try {
                value = valueParser.apply(split[1]);
            } catch (Throwable t) {
                throw new RuntimeException(String.format("Couldn't parse value from line: '%s'", line), t);
            }

            mapBuilder.put(key, value);
            return true;
        }

        @Override
        public Map<K, V> getResult() {
            return mapBuilder.build();
        }
    }
}
