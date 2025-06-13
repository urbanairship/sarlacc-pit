package com.urbanairship.sarlacc.client.functional;

import com.google.common.collect.ImmutableList;
import com.urbanairship.sarlacc.client.UpdateService;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.apache.commons.lang.RandomStringUtils.randomAlphanumeric;

public class TestMaxFailures {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBasicOperation() throws BrokenBarrierException, InterruptedException {
        expectedException.expect(IllegalStateException.class);

        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean shouldFail = new AtomicBoolean(false);

        final UpdateService<String, List<String>> service = UpdateService.<String, String>listServiceBuilder()
                .setServiceName("test")
                .setConfigSource(new BlockingConfigSource(shouldFail, barrier))
                .setMaxFailures(1)
                .setUpdateProcessor(ImmutableList::of)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .build();

        service.startAsync().awaitRunning();
        shouldFail.set(true);
        barrier.await();
        barrier.await();

        service.getUpdatingCollection().getFirst();
    }

    @Test
    public void testReset() throws BrokenBarrierException, InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        barrier.reset();
        final AtomicBoolean shouldFail = new AtomicBoolean(false);

        final UpdateService<String, List<String>> service = UpdateService.<String, String>listServiceBuilder()
                .setServiceName("test")
                .setConfigSource(new BlockingConfigSource(shouldFail, barrier))
                .setMaxFailures(1)
                .setUpdateProcessor(ImmutableList::of)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .build();

        service.startAsync().awaitRunning();
        shouldFail.set(true);
        barrier.await();
        barrier.await();

        try {
            service.getUpdatingCollection().getFirst();
            Assert.fail("Expected get to fail");
        } catch (IllegalStateException e) {
            //Expected
        }

        shouldFail.set(false);
        barrier.await();
        barrier.await();

        service.getUpdatingCollection().getFirst();
    }

    @Test
    @Ignore("Flappy")
    public void testMultipleFailures() throws BrokenBarrierException, InterruptedException {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final AtomicBoolean shouldFail = new AtomicBoolean(false);

        final UpdateService<String, List<String>> service = UpdateService.<String, String>listServiceBuilder()
                .setServiceName("test")
                .setConfigSource(new BlockingConfigSource(shouldFail, barrier))
                .setMaxFailures(2)
                .setUpdateProcessor(ImmutableList::of)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .build();

        service.startAsync().awaitRunning();
        shouldFail.set(true);
        barrier.await();
        barrier.await();

        service.getUpdatingCollection().getFirst();
        barrier.await();
        barrier.await();

        try {
            service.getUpdatingCollection().getFirst();
            Assert.fail("Expected get to fail");
        } catch (IllegalStateException e) {
            //Expected
        }

        shouldFail.set(false);
        barrier.await();
        barrier.await();

        service.getUpdatingCollection().getFirst();
    }

    private static class BlockingConfigSource implements ConfigSource<String> {
        private final AtomicBoolean shouldFail;
        private final CyclicBarrier barrier;

        private BlockingConfigSource(AtomicBoolean shouldFail, CyclicBarrier barrier) {
            this.shouldFail = shouldFail;
            this.barrier = barrier;
        }

        @Override
        public Optional<Update<String>> fetchIfNewer(long ifNewerThan) throws IOException {
            try {
                barrier.await();
            } catch (Exception e) {
                throw new IOException(e);
            }

            return Optional.of(fetch());
        }

        @Override
        public Update<String> fetch() throws IOException {
            if (shouldFail.get()) {
                throw new IOException("welp");
            } else {
                return new Update<>(0, randomAlphanumeric(100));
            }
        }
    }
}
