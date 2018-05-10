package com.urbanairship.sarlacc.client.functional;

import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import com.urbanairship.sarlacc.client.source.FileConfigSource;
import com.urbanairship.sarlacc.client.util.TestUtil;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;

import static com.urbanairship.sarlacc.client.util.TestUtil.assertEqualsWithSecondPrecision;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestFileConfigSource {
    @Test
    public void testUnconditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);

        File blacklistFile = getTestBlacklistFile(blacklist);
        ConfigSource<InputStream> configSource = new FileConfigSource(blacklistFile.getAbsolutePath(), false);
        final long ctime = blacklistFile.lastModified();

        Update<InputStream> update = configSource.fetch();

        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(update.newVal)));
        assertEquals(blacklist, got);
        assertEqualsWithSecondPrecision(ctime, update.version);
    }

    @Test
    public void testOkConditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        File blacklistFile = getTestBlacklistFile(blacklist);
        ConfigSource<InputStream> configSource = new FileConfigSource(blacklistFile.getAbsolutePath(), false);

        Optional<Update<InputStream>> update = configSource.fetchIfNewer(ctime - 1000);
        assertTrue(update.isPresent());

        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(update.get().newVal)));
        assertEquals(blacklist, got);
    }

    @Test
    public void testNotModifiedConditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        File blacklistFile = getTestBlacklistFile(blacklist);
        ConfigSource<InputStream> configSource = new FileConfigSource(blacklistFile.getAbsolutePath(), false);

        Optional<Update<InputStream>> update = configSource.fetchIfNewer(ctime + 1000);
        assertFalse(update.isPresent());
    }
    @Test
    public void testGzipUnconditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);

        File blacklistFile = getTestGZippedBlacklistFile(blacklist);
        final long ctime = blacklistFile.lastModified();
        ConfigSource<InputStream> configSource = new FileConfigSource(blacklistFile.getAbsolutePath(), true);

        Update<InputStream> update = configSource.fetch();

        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(update.newVal)));
        assertEquals(blacklist, got);
        assertEqualsWithSecondPrecision(ctime, update.version);
    }

    @Test
    public void testGzipOkConditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        File blacklistFile = getTestGZippedBlacklistFile(blacklist);
        ConfigSource<InputStream> configSource = new FileConfigSource(blacklistFile.getAbsolutePath(), true);

        Optional<Update<InputStream>> update = configSource.fetchIfNewer(ctime - 1000);
        assertTrue(update.isPresent());

        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(update.get().newVal)));
        assertEquals(blacklist, got);
    }

    @Test
    public void testGzipNotModifiedConditionalFetch() throws Exception {
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(100);
        final long ctime = System.currentTimeMillis();

        File blacklistFile = getTestGZippedBlacklistFile(blacklist);
        ConfigSource<InputStream> configSource = new FileConfigSource(blacklistFile.getAbsolutePath(), true);

        Optional<Update<InputStream>> update = configSource.fetchIfNewer(ctime + 1000);
        assertFalse(update.isPresent());
    }

    private File getTestBlacklistFile(Set<String> blacklist) throws Exception {
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".txt");
        temp.deleteOnExit();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(temp)));

        writer.write(StringUtils.join(blacklist, "\n"));

        writer.close();

        return temp;
    }

    private File getTestGZippedBlacklistFile(Set<String> blacklist) throws Exception {
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".txt.gz");
        temp.deleteOnExit();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(temp))));

        writer.write(StringUtils.join(blacklist, "\n"));

        writer.close();

        return temp;
    }
}
