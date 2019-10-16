package com.urbanairship.sarlacc.client.functional;


import com.urbanairship.sarlacc.client.UpdateService;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.processor.DelimitedStreamMapProcessor;
import com.urbanairship.sarlacc.client.processor.flatfile.ImmutableArrayStringSetLineProcessor;
import com.urbanairship.sarlacc.client.processor.flatfile.MultilinePlaintextUpdateProcessor;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import com.urbanairship.sarlacc.client.structures.defaults.ContainsAllSet;
import com.urbanairship.sarlacc.client.util.TestUtil;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.verification.Times;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TestUpdateService {

    @Mock
    private ConfigSource<InputStream> configSource;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testBasicOperation() throws Exception {
        UpdateService<InputStream, Set<String>> updateService = UpdateService.<InputStream, String>setServiceBuilder()
                .setServiceName("FOO")
                .setUpdateProcessor(new MultilinePlaintextUpdateProcessor<>(ImmutableArrayStringSetLineProcessor.SUPPLIER))
                .setConfigSource(configSource)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .build();

        long initMTime = 2500;
        long updatedMtime = 5000;
        Set<String> blacklistSet = TestUtil.getBlacklistAsSet(500);
        Set<String> updatedBlacklistSet = TestUtil.getBlacklistAsSet(1000);

        InputStream blacklistStream = new ByteArrayInputStream(StringUtils.join(blacklistSet, "\n").getBytes("UTF-8"));
        InputStream updatedBlacklistStream = new ByteArrayInputStream(StringUtils.join(updatedBlacklistSet, "\n").getBytes("UTF-8"));

        // First check will say no change, second will return the new blacklist update
        when(configSource.fetch()).thenReturn(new Update<>(initMTime, blacklistStream));
        when(configSource.fetchIfNewer(initMTime))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Update<>(updatedMtime, updatedBlacklistStream)));

        when(configSource.fetchIfNewer(updatedMtime)).thenReturn(Optional.empty());

        updateService.startAsync().awaitRunning();

        Set<String> wrappedCollection = updateService.getUpdatingCollection();

        assertEquals(blacklistSet.size(), wrappedCollection.size());
        assertEquals(blacklistSet, wrappedCollection);

        Thread.sleep(110);

        // After the first check, there should still be no change
        assertEquals(blacklistSet.size(), wrappedCollection.size());
        assertEquals(blacklistSet, wrappedCollection);

        Thread.sleep(100);

        // After the second check, we should have updated
        assertEquals(updatedBlacklistSet.size(), wrappedCollection.size());
        assertEquals(updatedBlacklistSet, wrappedCollection);

        updateService.stopAsync().awaitTerminated();
        verify(configSource, new Times(2)).fetchIfNewer(anyLong());
    }

    @Test
    public void testFallback() throws Exception {
        UpdateService<InputStream, Set<String>> updateService = UpdateService.<InputStream, String>setServiceBuilder()
                .setServiceName("FOO")
                .setUpdateProcessor(new MultilinePlaintextUpdateProcessor<>(ImmutableArrayStringSetLineProcessor.SUPPLIER))
                .setConfigSource(configSource)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .setFallbackValue(new ContainsAllSet<>(),0)
                .build();

        // First check will say no change, second will return the new blacklist update
        when(configSource.fetch()).thenThrow(new IOException("womp"));
        when(configSource.fetchIfNewer(anyLong()))
                .thenReturn(Optional.empty());

        updateService.startAsync().awaitRunning();

        Set<String> wrappedCollection = updateService.getUpdatingCollection();

        Assert.assertTrue(wrappedCollection.contains(RandomStringUtils.randomAlphanumeric(10)));
        Assert.assertTrue(wrappedCollection.contains(RandomStringUtils.randomAlphanumeric(10)));
        Assert.assertTrue(wrappedCollection.contains(RandomStringUtils.randomAlphanumeric(10)));

        updateService.stopAsync().awaitTerminated();
    }

    @Test
    public void testBasicOperationOfMap() throws Exception {
        UpdateService<InputStream, Map<Integer, String>> updateService = UpdateService.<InputStream, Integer, String>mapServiceBuilder()
                .setServiceName("bar")
                .setUpdateProcessor(DelimitedStreamMapProcessor.supplierBuilder()
                        .setDelimiter(",")
                        .setKeyParser(Integer::parseInt)
                        .build()
                        .get())
                .setConfigSource(configSource)
                .setFetchInterval(100, TimeUnit.MILLISECONDS)
                .build();

        long initMTime = 2500;
        long updatedMtime = 5000;
        Map<Integer, String> blacklistSet = TestUtil.getRandomMapOfSize(500);
        Map<Integer, String> updatedBlacklistSet = TestUtil.getRandomMapOfSize(1000);

        InputStream blacklistStream = new ByteArrayInputStream(StringUtils.join(blacklistSet.entrySet().stream().map(e -> e.getKey() + "," + e.getValue()).collect(Collectors.toList()), "\n").getBytes("UTF-8"));
        InputStream updatedBlacklistStream = new ByteArrayInputStream(StringUtils.join(updatedBlacklistSet.entrySet().stream().map(e -> e.getKey() + "," + e.getValue()).collect(Collectors.toList()), "\n").getBytes("UTF-8"));

        // First check will say no change, second will return the new blacklist update
        when(configSource.fetch()).thenReturn(new Update<>(initMTime, blacklistStream));
        when(configSource.fetchIfNewer(initMTime))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(new Update<>(updatedMtime, updatedBlacklistStream)));

        when(configSource.fetchIfNewer(updatedMtime)).thenReturn(Optional.empty());

        updateService.startAsync().awaitRunning();

        Map<Integer, String> wrappedCollection = updateService.getUpdatingCollection();

        assertEquals(blacklistSet.size(), wrappedCollection.size());
        assertEquals(blacklistSet, wrappedCollection);

        Thread.sleep(110);

        // After the first check, there should still be no change
        assertEquals(blacklistSet.size(), wrappedCollection.size());
        assertEquals(blacklistSet, wrappedCollection);

        Thread.sleep(100);

        // After the second check, we should have updated
        assertEquals(updatedBlacklistSet.size(), wrappedCollection.size());
        assertEquals(updatedBlacklistSet, wrappedCollection);

        updateService.stopAsync().awaitTerminated();
        verify(configSource, new Times(2)).fetchIfNewer(anyLong());
    }
}
