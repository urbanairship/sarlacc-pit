package com.urbanairship.sarlacc.client;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractIdleService;
import com.urbanairship.sarlacc.client.metrics.JmxMetricNamer;
import com.urbanairship.sarlacc.client.metrics.MetricNamer;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.processor.UpdateProcessor;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import com.urbanairship.sarlacc.client.structures.container.UpdatingCollection;
import com.urbanairship.sarlacc.client.structures.container.UpdatingList;
import com.urbanairship.sarlacc.client.structures.container.UpdatingMap;
import com.urbanairship.sarlacc.client.structures.container.UpdatingSet;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * A single-thread service which exposes and keeps updated an in memory data structure
 * using an external source.
 *
 * @param <S> The raw output type of the backing config source. Flat sources such as HTTP and File
 *            use InputStream, SQL uses ResultSet, etc.
 * @param <C> The type of data structure maintained by the service instance.
 */
public class UpdateService<S, C> extends AbstractIdleService {
    private static final Logger log = LogManager.getLogger(UpdateService.class);

    private static final Closeable NOOP_CLOSEABLE = () -> {};

    private final C updatingCollection;
    private final AtomicReference<C> reference;

    private final AtomicReference<Long> currentVersion;
    private final UpdateProcessor<S, ? extends C> updateProcessor;
    private final ConfigSource<S> configSource;
    private final String serviceName;
    private final long fetchIntervalMillis;
    private final ScheduledExecutorService executorService;

    private final FetchFailureCallback fetchFailureCallback;
    private final UpdateCallback<C> updateCallback;
    private final AtomicInteger successiveFailures = new AtomicInteger(0);

    private final AtomicReference<Long> lastSuccessfulCheck;

    private final Optional<C> fallbackValue;
    private final long fallbackVersion;

    private final Optional<Metrics> metrics;

    private final AtomicReference<String> lastError = new AtomicReference<>(null);

    private UpdateService(ConfigSource<S> configSource, UpdateProcessor<S, ? extends C> updateProcessor,
                          AtomicReference<C> reference, C updatingCollection,
                          final long fetchIntervalMillis, String serviceName, Optional<MetricRegistry> metricRegistry,
                          MetricNamer metricNamer, FetchFailureCallback fetchFailureCallback, UpdateCallback<C> updateCallback,
                          Optional<C> fallbackValue, long fallbackVersion) {

        this.configSource = configSource;
        this.updateProcessor = updateProcessor;
        this.fetchIntervalMillis = fetchIntervalMillis;
        this.serviceName = serviceName;
        this.reference = reference;
        this.updatingCollection = updatingCollection;
        this.fetchFailureCallback = fetchFailureCallback;
        this.updateCallback = updateCallback;
        this.fallbackValue = fallbackValue;
        this.fallbackVersion = fallbackVersion;

        this.currentVersion = new AtomicReference<>(0L);
        this.executorService = Executors.newScheduledThreadPool(1);
        this.lastSuccessfulCheck = new AtomicReference<>(System.currentTimeMillis());

        this.metrics = metricRegistry.map(registry -> new Metrics(registry, metricNamer));
    }

    public C getUpdatingCollection() {
        return updatingCollection;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Instant getLastSuccessfulCheck() {
        return Instant.ofEpochMilli(lastSuccessfulCheck.get());
    }

    public long getCurrentVersion() {
        return currentVersion.get();
    }

    public Optional<String> getLastError() {
        return Optional.ofNullable(lastError.get());
    }

    @Override
    protected synchronized void startUp() throws Exception {
        try (Update<S> initialFetch = configSource.fetch()) {
            C initialVal = updateProcessor.process(initialFetch.newVal);

            reference.set(initialVal);
            currentVersion.set(initialFetch.version);

            try {
                updateCallback.onUpdate(Optional.empty(), Optional.empty(), initialVal, initialFetch.version);
            } catch (Throwable t) {
                log.error("Update callback threw!", t);
                metrics.ifPresent(metrics -> metrics.updateCallbackFailures.inc());
            }
        } catch (IOException ioe) {
            if (fallbackValue.isPresent()) {
                reference.set(fallbackValue.get());
                currentVersion.set(fallbackVersion);
                metrics.ifPresent(metrics -> metrics.getFallbackUsed().inc());
                log.error("Initial fetch failed and fallback used for service: " + serviceName, ioe);
            } else {
                throw new Exception("Exception while starting update service for " + serviceName, ioe);
            }
        } catch (Exception e) {
            throw new Exception("Exception while starting update service for " + serviceName, e);
        }

        executorService.scheduleAtFixedRate(new FetcherRunnable(), fetchIntervalMillis, fetchIntervalMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void shutDown() {
        executorService.shutdown();
    }

    private class FetcherRunnable implements Runnable {
        @Override
        public void run() {
            try {
                if (log.isDebugEnabled()) {
                    log.debug("Performing update run for " + serviceName);
                }
                // This shouldn't be called in an overlapping manner, but let's be paranoid.
                synchronized (UpdateService.this) {
                    final Optional<Update<S>> maybeUpdate;
                    try (Closeable checkTime = getTimer(Metrics::getCheckTimer)) {
                        if (fallbackValue.isPresent() && fallbackValue.get() == reference.get()) {
                            maybeUpdate = Optional.of(configSource.fetch());
                        } else {
                            maybeUpdate = configSource.fetchIfNewer(currentVersion.get());
                        }
                    }
                    successiveFailures.set(0);

                    if (maybeUpdate.isPresent()) {
                        try (final Update<S> update = maybeUpdate.get()) {
                            final C newVal;
                            final long newVersion;
                            final C oldVal;
                            final long oldVersion;
                            try (final Closeable processTime = getTimer(Metrics::getFetchAndProcessTimer)) {
                                newVal = updateProcessor.process(update.newVal);
                                newVersion = update.version;
                                oldVal = reference.get();
                                oldVersion = currentVersion.get();

                                reference.set(newVal);
                                currentVersion.set(newVersion);
                            }

                            log.info("Updated backing value for " + serviceName);

                            try (final Closeable callbackTimer = getTimer(Metrics::getUpdateCallbackTimer)) {
                                final Optional<C> oldValForCallback;
                                final Optional<Long> oldVersionForCallback;
                                if (fallbackValue.isPresent() && fallbackValue.get() == oldVal) {
                                    oldValForCallback = Optional.empty();
                                    oldVersionForCallback = Optional.empty();
                                } else {
                                    oldValForCallback = Optional.of(oldVal);
                                    oldVersionForCallback = Optional.of(oldVersion);
                                }

                                updateCallback.onUpdate(oldValForCallback, oldVersionForCallback, newVal, newVersion);
                            } catch (Throwable t) {
                                log.error("Update callback threw!", t);
                                metrics.ifPresent(metrics -> metrics.getUpdateCallbackFailures().inc());
                            }
                        }
                    } else if (log.isDebugEnabled()) {
                        log.debug(String.format("Checked for update for '%s', but everything was up to date.", serviceName));
                    }

                    lastError.set(null);
                    lastSuccessfulCheck.set(System.currentTimeMillis());
                }
            } catch (Throwable t) {
                final int failures = successiveFailures.incrementAndGet();

                log.error("Error while checking for update for " + serviceName, t);
                lastError.set(t.getMessage());
                metrics.ifPresent(metrics -> metrics.getErrorMeter().mark());

                try (Closeable fetchFailCallbackTime = getTimer(Metrics::getFetchFailCallbackTimer)) {
                    fetchFailureCallback.onFetchFailure(currentVersion.get(), lastSuccessfulCheck.get(), failures, t);
                } catch (Throwable fetchThrown) {
                    log.error("Fetch failure callback threw!", fetchThrown);
                    metrics.ifPresent(metrics -> metrics.getFetchFailureCallbackFailures().inc());
                }
            }
        }
    }

    public static <S, K, V> Builder<S, Map<K, V>> mapServiceBuilder() {
        AtomicReference<Map<K, V>> ref = new AtomicReference<>(ImmutableMap.<K, V>of());
        return new Builder<>(ref, new UpdatingMap<>(ref));
    }

    public static <S, T> Builder<S, Set<T>> setServiceBuilder() {
        AtomicReference<Set<T>> ref = new AtomicReference<>(ImmutableSet.<T>of());
        return new Builder<>(ref, new UpdatingSet<>(ref));
    }

    public static <S, T> Builder<S, List<T>> listServiceBuilder() {
        AtomicReference<List<T>> ref = new AtomicReference<>(ImmutableList.<T>of());
        return new Builder<>(ref, new UpdatingList<>(ref));
    }

    public static class Builder<S, D> {
        private final AtomicReference<D> backingRef;
        private final UpdatingCollection<D> collectionWrapper;

        private UpdateProcessor<S, ? extends D> updateProcessor;
        private ConfigSource<S> configSource;
        private String serviceName;
        private long fetchIntervalMillis;

        private Optional<D> fallbackValue = Optional.empty();
        private long fallbackVersion = Long.MIN_VALUE;

        private Optional<MetricRegistry> metricRegistry = Optional.empty();
        private MetricNamer metricNamer = new JmxMetricNamer();

        private UpdateCallback<D> updateCallback = (p, pm, c, cm) -> {};
        private FetchFailureCallback fetchFailureCallback = (l, l2, i, t) -> {};

        private Builder(AtomicReference<D> backingRef, UpdatingCollection<D> collectionWrapper) {
            this.backingRef = backingRef;
            this.collectionWrapper = collectionWrapper;
        }

        public <D1 extends D> Builder<S, D> setUpdateProcessor(UpdateProcessor<S, D1> updateProcessor) {
            this.updateProcessor = updateProcessor;
            return this;
        }

        public Builder<S, D> setConfigSource(ConfigSource<S> configSource) {
            this.configSource = configSource;
            return this;
        }

        public Builder<S, D> setServiceName(String serviceName) {
            this.serviceName = serviceName;
            return this;
        }

        public Builder<S, D> setMetricRegistry(MetricRegistry metricRegistry) {
            this.metricRegistry = Optional.of(metricRegistry);
            return this;
        }

        public Builder<S, D> setMetricNamer(MetricNamer metricNamer) {
            this.metricNamer = metricNamer;
            return this;
        }

        public Builder<S, D> setFetchInterval(long fetchInterval, TimeUnit timeUnit) {
            this.fetchIntervalMillis = timeUnit.toMillis(fetchInterval);
            return this;
        }

        public Builder<S, D> setUpdateCallback(UpdateCallback<D> updateCallback) {
            this.updateCallback = updateCallback;
            return this;
        }

        public Builder<S, D> setFetchFailureCallback(FetchFailureCallback fetchFailureCallback) {
            this.fetchFailureCallback = fetchFailureCallback;
            return this;
        }

        public Builder<S, D> setFallbackValue(D fallbackValue, long fallbackVersion) {
            this.fallbackValue = Optional.of(fallbackValue);
            this.fallbackVersion = fallbackVersion;
            return this;
        }

        public UpdateService<S, D> build() {
            Preconditions.checkNotNull(serviceName);
            Preconditions.checkNotNull(updateProcessor);
            Preconditions.checkNotNull(configSource);
            Preconditions.checkArgument(fetchIntervalMillis > 0);

            final UpdateService<S, D> updateService = new UpdateService<>(configSource, updateProcessor, backingRef,
                    collectionWrapper.getAsCollectionType(), fetchIntervalMillis, serviceName, metricRegistry, metricNamer,
                    fetchFailureCallback, updateCallback, fallbackValue, fallbackVersion);

            collectionWrapper.setUpdateService(updateService);

            return updateService;
        }
    }

    private Closeable getTimer(Function<Metrics, Timer> timerGetter) {
        return metrics.map(timerGetter).map(timer -> (Closeable) timer.time()).orElse(NOOP_CLOSEABLE);
    }

    private class Metrics {
        private final Timer checkTimer;
        private final Timer fetchAndProcessTimer;
        private final Timer updateCallbackTimer;
        private final Timer fetchFailCallbackTimer;
        private final Meter errorMeter;
        private final Counter updateCallbackFailures;
        private final Counter fetchFailureCallbackFailures;
        private final Counter fallbackUsed;

        private Metrics(MetricRegistry metricRegistry, MetricNamer metricNamer) {
            this.checkTimer = metricRegistry.timer(metricNamer.name(UpdateService.class, "Check Time", Optional.of(serviceName)));
            this.fetchAndProcessTimer = metricRegistry.timer(metricNamer.name(UpdateService.class, "Fetch and Process Time", Optional.of(serviceName)));
            this.updateCallbackTimer = metricRegistry.timer(metricNamer.name(UpdateService.class, "Update Callback Time", Optional.of(serviceName)));
            this.fetchFailCallbackTimer = metricRegistry.timer(metricNamer.name(UpdateService.class, "Fetch Fail Callback Time", Optional.of(serviceName)));
            this.errorMeter = metricRegistry.meter(metricNamer.name(UpdateService.class, "Errors Checking For Update", Optional.of(serviceName)));
            this.updateCallbackFailures = metricRegistry.counter(metricNamer.name(UpdateService.class, "Update Callback Errors", Optional.of(serviceName)));
            this.fetchFailureCallbackFailures = metricRegistry.counter(metricNamer.name(UpdateService.class, "Fetch Failure Callback Errors", Optional.of(serviceName)));
            this.fallbackUsed = metricRegistry.counter(metricNamer.name(UpdateService.class, "Fallback Used", Optional.of(serviceName)));

            metricRegistry.register(metricNamer.name(UpdateService.class, "Check Age", Optional.of(serviceName)),
                    (Gauge<Long>) () -> System.currentTimeMillis() - lastSuccessfulCheck.get());

            metricRegistry.register(metricNamer.name(UpdateService.class, "Value Version", Optional.of(serviceName)),
                    (Gauge<Long>) currentVersion::get);
        }

        private Timer getCheckTimer() {
            return checkTimer;
        }

        private Timer getFetchAndProcessTimer() {
            return fetchAndProcessTimer;
        }

        private Timer getUpdateCallbackTimer() {
            return updateCallbackTimer;
        }

        private Timer getFetchFailCallbackTimer() {
            return fetchFailCallbackTimer;
        }

        private Meter getErrorMeter() {
            return errorMeter;
        }

        private Counter getUpdateCallbackFailures() {
            return updateCallbackFailures;
        }

        private Counter getFetchFailureCallbackFailures() {
            return fetchFailureCallbackFailures;
        }

        private Counter getFallbackUsed() {
            return fallbackUsed;
        }
    }
}
