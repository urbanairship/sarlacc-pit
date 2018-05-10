package com.urbanairship.sarlacc.client.processor.sql;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.urbanairship.sarlacc.client.processor.UpdateProcessor;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class SqlUpdateProcessor<C> implements UpdateProcessor<ResultSet, C> {
    private final Supplier<ResultSetProcessor<C>> supplier;

    public SqlUpdateProcessor(Supplier<ResultSetProcessor<C>> supplier) {
        this.supplier = supplier;
    }

    @Override
    public C process(ResultSet input) throws IOException {
        try {
            final ResultSetProcessor<C> processor = supplier.get();
            while (input.next()) {
                processor.process(input);
            }
            return processor.getDataStructure();
        } catch (SQLException e) {
            throw new IOException(e);
        }
    }

    public static <T> SqlUpdateProcessor<List<T>> list(final Function<ResultSet, T> parseFunc) {
        final Supplier<ResultSetProcessor<List<T>>> supplier = () -> new ResultSetProcessor<List<T>>() {
            private final ImmutableList.Builder<T> builder = ImmutableList.builder();

            @Override
            public void process(ResultSet resultSet) {
                T item = Preconditions.checkNotNull(parseFunc.apply(resultSet));
                builder.add(item);
            }

            @Override
            public List<T> getDataStructure() {
                return builder.build();
            }
        };

        return new SqlUpdateProcessor<>(supplier);
    }

    public static <T> SqlUpdateProcessor<Set<T>> set(final Function<ResultSet, T> parseFunc) {
        final Supplier<ResultSetProcessor<Set<T>>> supplier = () -> new ResultSetProcessor<Set<T>>() {
            private final ImmutableSet.Builder<T> builder = ImmutableSet.builder();

            @Override
            public void process(ResultSet resultSet) {
                T item = Preconditions.checkNotNull(parseFunc.apply(resultSet));
                builder.add(item);
            }

            @Override
            public Set<T> getDataStructure() {
                return builder.build();
            }
        };

        return new SqlUpdateProcessor<>(supplier);
    }

    public static <K,V> SqlUpdateProcessor<Map<K,V>> map(final Function<ResultSet, Map.Entry<K,V>> parseFunc) {
        final Supplier<ResultSetProcessor<Map<K, V>>> supplier = () -> new ResultSetProcessor<Map<K, V>>() {
            private final ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();

            @Override
            public void process(ResultSet resultSet) {
                Map.Entry<K, V> entry = Preconditions.checkNotNull(parseFunc.apply(resultSet));
                builder.put(entry);
            }

            @Override
            public Map<K, V> getDataStructure() {
                return builder.build();
            }
        };

        return new SqlUpdateProcessor<>(supplier);
    }
}
