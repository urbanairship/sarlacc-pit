package com.urbanairship.sarlacc.client.functional;

import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import com.urbanairship.sarlacc.client.source.HttpSource;
import com.urbanairship.sarlacc.client.util.TestHttpServer;
import com.urbanairship.sarlacc.client.util.TestUtil;
import org.junit.Rule;
import org.junit.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Optional;
import java.util.Set;

import static com.urbanairship.sarlacc.client.util.TestUtil.assertEqualsWithSecondPrecision;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestHttpConfigSource {
    @Rule
    public TestHttpServer sourceServer = new TestHttpServer();

    @Test
    public void testUnconditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        sourceServer.setHandler(TestUtil.buildHandler(blacklist, ctime));

        ConfigSource<InputStream> configSource = new HttpSource(sourceServer.getLocalAddr());
        Update<InputStream> update = configSource.fetch();

        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(update.newVal)));
        assertEquals(blacklist, got);
        assertEqualsWithSecondPrecision(ctime, update.version);
    }

    @Test
    public void testOkConditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        sourceServer.setHandler(TestUtil.buildHandler(blacklist, ctime));

        ConfigSource<InputStream> configSource = new HttpSource(sourceServer.getLocalAddr());
        Optional<Update<InputStream>> update = configSource.fetchIfNewer(ctime - 1000);
        assertTrue(update.isPresent());

        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(update.get().newVal)));
        assertEquals(blacklist, got);
    }

    @Test
    public void testNotModifiedConditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        sourceServer.setHandler(TestUtil.buildHandler(blacklist, ctime));

        ConfigSource<InputStream> configSource = new HttpSource(sourceServer.getLocalAddr());
        Optional<Update<InputStream>> update = configSource.fetchIfNewer(ctime + 1000);
        assertFalse(update.isPresent());
    }
}
