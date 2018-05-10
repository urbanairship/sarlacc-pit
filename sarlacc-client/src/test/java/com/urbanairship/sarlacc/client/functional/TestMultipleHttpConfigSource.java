package com.urbanairship.sarlacc.client.functional;


import com.google.common.collect.Sets;
import com.google.common.io.CharStreams;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import com.urbanairship.sarlacc.client.source.MultipleHttpConfigSource;
import com.urbanairship.sarlacc.client.util.TestHttpServer;
import com.urbanairship.sarlacc.client.util.TestUtil;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class TestMultipleHttpConfigSource {

    @Rule
    public TestHttpServer sourceServer1 = new TestHttpServer();
    @Rule
    public TestHttpServer sourceServer2 = new TestHttpServer();
    @Rule
    public TestHttpServer sourceServer3 = new TestHttpServer();
    @Rule
    public TestHttpServer sourceServer4 = new TestHttpServer();

    @Test
    public void testMultipleSources() throws Exception {
        Logger.getRootLogger().setLevel(Level.ERROR);
        final Set<String> blacklist = TestUtil.getBlacklistAsSet(50);
        final long ctime = System.currentTimeMillis();

        sourceServer4.setHandler(TestUtil.buildHandler(blacklist, ctime));
        sourceServer2.setHandler(buildBrokenHandler());
        sourceServer3.setHandler(buildBrokenHandler());
        sourceServer1.setHandler(buildBrokenHandler());

        List<String> connectionUrls = new ArrayList<>();
        connectionUrls.add(sourceServer1.getLocalAddr());
        connectionUrls.add(sourceServer2.getLocalAddr());
        connectionUrls.add(sourceServer4.getLocalAddr());
        connectionUrls.add(sourceServer3.getLocalAddr());

        ConfigSource<InputStream> multipleHttpSource = new MultipleHttpConfigSource(connectionUrls);

        Update<InputStream> fetch = multipleHttpSource.fetch();
        Set<String> got = Sets.newHashSet(CharStreams.readLines(new InputStreamReader(fetch.newVal)));
        assertEquals(blacklist, got);
    }

    private Handler buildBrokenHandler() {
        return new AbstractHandler() {
            @Override
            public void handle(String s, Request request, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException, ServletException {
                httpServletResponse.setStatus(500);
            }
        };
    }
}
