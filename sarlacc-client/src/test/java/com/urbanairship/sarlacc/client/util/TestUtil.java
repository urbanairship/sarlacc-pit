package com.urbanairship.sarlacc.client.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import org.apache.commons.lang.RandomStringUtils;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.assertEquals;

public class TestUtil {
    private TestUtil() {
    }

    public static Set<String> getBlacklistAsSet(int size) {
        Preconditions.checkArgument(size > 0);
        Set<String> blacklist = Sets.newHashSet();

        for (int i = 0; i < size; i++) {
            blacklist.add(RandomStringUtils.randomAlphanumeric(22));
        }

        return blacklist;
    }

    public static void assertEqualsWithSecondPrecision(long tExpected, long tActual) {
        assertEquals(TimeUnit.SECONDS.convert(tExpected, TimeUnit.MILLISECONDS),
                TimeUnit.SECONDS.convert(tActual, TimeUnit.MILLISECONDS));
    }

    public static Handler buildHandler(final Set<String> served, final long mtime) {
        return new AbstractHandler() {
            @Override
            public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
                baseRequest.setHandled(true);

                if (request.getDateHeader("If-Modified-Since") > mtime) {
                    response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                    return;
                }

                response.addDateHeader("Last-Modified", mtime);
                response.setHeader("Content-Encoding", "gzip");
                response.setStatus(200);

                OutputStream outputStream = new GZIPOutputStream(response.getOutputStream());

                for (String item : served) {
                    outputStream.write(item.getBytes());
                    outputStream.write('\n');
                }

                outputStream.flush();
                outputStream.close();
            }
        };
    }
}
