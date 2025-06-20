package com.urbanairship.sarlacc.client.callbacks;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.urbanairship.sarlacc.client.UpdateCallback;
import com.urbanairship.sarlacc.client.UpdateService;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.processor.UpdateProcessor;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

public class TestUpdateCallback {
    @Mock
    ConfigSource<String> configSource;

    @Mock
    UpdateProcessor<String, Set<String>> updateProcessor;

    private UpdateService<String, Set<String>> updateService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {
        if (updateService != null && updateService.isRunning()) {
            updateService.stopAsync().awaitTerminated();
        }
    }

    @Test
    public void testCalledCorrectly() throws Exception {
        final Semaphore semaphore = new Semaphore(0);
        final TestCallback updateCallback = new TestCallback(semaphore);

        updateService = UpdateService.<String, String>setServiceBuilder()
                .setServiceName(RandomStringUtils.randomAlphanumeric(10))
                .setUpdateProcessor(updateProcessor)
                .setConfigSource(configSource)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .setUpdateCallback(updateCallback)
                .build();

        final long initTime = System.currentTimeMillis();
        final String initStr = "init";
        final Optional<Update<String>> noUpdate = Optional.empty();

        when(updateProcessor.process(initStr)).thenReturn(ImmutableSet.of(initStr));
        when(configSource.fetch()).thenReturn(new Update<>(initTime, initStr));

        final long updateTime = initTime + 100;
        final String updateStr = "v1";
        when(updateProcessor.process(updateStr)).thenReturn(ImmutableSet.of(updateStr));

        when(configSource.fetchIfNewer(anyLong()))
                .thenReturn(noUpdate)
                .thenReturn(noUpdate)
                .thenReturn(Optional.of(new Update<>(updateTime, updateStr)));

        updateService.startAsync().awaitRunning();
        semaphore.acquire(2);

        final List<ArgSet> callHistory = updateCallback.getCallHistory();
        assertEquals(2, callHistory.size());

        final ArgSet expectedInitCall = new ArgSet(Optional.empty(), Optional.empty(), ImmutableSet.of(initStr), initTime);
        assertEquals(expectedInitCall, callHistory.get(0));

        final ArgSet expectedUpdateCall = new ArgSet(Optional.of(ImmutableSet.of(initStr)), Optional.of(initTime),
                ImmutableSet.of(updateStr), updateTime);
        assertEquals(expectedUpdateCall, callHistory.get(1));
    }

    private static class TestCallback implements UpdateCallback<Set<String>> {
        private final ImmutableList.Builder<ArgSet> calls = ImmutableList.builder();
        private final Semaphore semaphore;

        private TestCallback(Semaphore semaphore) {
            this.semaphore = semaphore;
        }

        @Override
        public void onUpdate(Optional<Set<String>> previous, Optional<Long> previousVersion, Set<String> current, long currentVersion) {
            calls.add(new ArgSet(previous, previousVersion, current, currentVersion));
            semaphore.release();
        }

        public List<ArgSet> getCallHistory() {
            return calls.build();
        }
    }

    private static class ArgSet {
        public final Optional<Set<String>> previous;
        public final Optional<Long> previousMtime;
        public final Set<String> current;
        public final long currentMtime;

        public ArgSet(Optional<Set<String>> previous, Optional<Long> previousMtime, Set<String> current, long currentMtime) {
            this.previous = previous;
            this.previousMtime = previousMtime;
            this.current = current;
            this.currentMtime = currentMtime;
        }

        @Override
        public String toString() {
            return "ArgSet{" +
                    "previous=" + previous +
                    ", previousMtime=" + previousMtime +
                    ", current=" + current +
                    ", currentMtime=" + currentMtime +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ArgSet)) return false;

            ArgSet argSet = (ArgSet) o;

            if (currentMtime != argSet.currentMtime) return false;
            if (previous != null ? !previous.equals(argSet.previous) : argSet.previous != null) return false;
            if (previousMtime != null ? !previousMtime.equals(argSet.previousMtime) : argSet.previousMtime != null)
                return false;
            return current != null ? current.equals(argSet.current) : argSet.current == null;
        }

        @Override
        public int hashCode() {
            int result = previous != null ? previous.hashCode() : 0;
            result = 31 * result + (previousMtime != null ? previousMtime.hashCode() : 0);
            result = 31 * result + (current != null ? current.hashCode() : 0);
            result = 31 * result + (int) (currentMtime ^ (currentMtime >>> 32));
            return result;
        }
    }
}
