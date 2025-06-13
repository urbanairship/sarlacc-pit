package com.urbanairship.sarlacc.client.callbacks;

import com.google.common.collect.ImmutableSet;
import com.urbanairship.sarlacc.client.FailureCallback;
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

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class TestFailureCallback {
    @Mock
    ConfigSource<String> configSource;

    @Mock
    UpdateProcessor<String, Set<String>> updateProcessor;

    @Mock
    FailureCallback failureCallback;

    private UpdateService<String, Set<String>> updateService;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        updateService = UpdateService.<String, String>setServiceBuilder()
                .setServiceName(RandomStringUtils.randomAlphanumeric(10))
                .setUpdateProcessor(updateProcessor)
                .setConfigSource(configSource)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .setFailureCallback(failureCallback)
                .build();
    }

    @After
    public void tearDown() throws Exception {
        if (updateService != null && updateService.isRunning()) {
            updateService.stopAsync().awaitTerminated();
        }
    }

    @Test
    public void testNotCalled() throws Exception {
        CountDownLatch latch = new CountDownLatch(10);

        when(updateProcessor.process(anyString())).thenReturn(ImmutableSet.<String>of());
        when(configSource.fetch())
                .then(new CountDownAnswer<>(latch, new Update<>(System.currentTimeMillis(), "")));
        when(configSource.fetchIfNewer(anyLong()))
                .then(new CountDownAnswer<>(latch, Optional.<Update<String>>empty()));

        updateService.startAsync().awaitRunning();

        latch.await();
        verifyNoMoreInteractions(failureCallback);
    }

    @Test
    public void testCalledCorrectly() throws Exception {
        final CountDownLatch latch = new CountDownLatch(3);
        final IOException ioException = new IOException();
        final CountDownAnswer<Optional<Update<String>>> answerOptional =
                new CountDownAnswer<Optional<Update<String>>>(latch, Optional.<Update<String>>empty());

        when(updateProcessor.process(anyString())).thenReturn(ImmutableSet.<String>of());
        final long version = System.currentTimeMillis();

        when(configSource.fetch())
                .then(new CountDownAnswer<>(latch, new Update<>(version, "")));
        when(configSource.fetchIfNewer(anyLong()))
                .then(answerOptional)
                .thenThrow(ioException)
                .thenThrow(ioException)
                .thenThrow(ioException)
                .then(answerOptional);

        updateService.startAsync().awaitRunning();
        latch.await();

        verify(failureCallback, times(3)).onFailure(any(), eq(ioException));
        verifyNoMoreInteractions(failureCallback);
    }

    // Make sure we correctly deal with the failure callback itself throwing
    @Test(timeout = 1000)
    public void testExplosion() throws Exception {
        final long timestamp = System.currentTimeMillis();
        final CountDownLatch latch = new CountDownLatch(3);
        final CountDownAnswer<Optional<Update<String>>> answerOptional =
                new CountDownAnswer<Optional<Update<String>>>(latch, Optional.<Update<String>>empty());

        doThrow(new NullPointerException()).when(failureCallback)
                .onFailure(any(), any(Throwable.class));

        when(updateProcessor.process(anyString())).thenReturn(ImmutableSet.<String>of());

        when(configSource.fetch())
                .then(new CountDownAnswer<>(latch, new Update<>(timestamp, "")));
        when(configSource.fetchIfNewer(anyLong()))
                .then(answerOptional)
                .thenThrow(new IOException())
                .then(answerOptional);

        updateService.startAsync().awaitRunning();
        latch.await();

        verify(failureCallback).onFailure(any(), any(Throwable.class));
    }
}
