package com.urbanairship.sarlacc.client.source;

import com.urbanairship.sarlacc.client.model.Update;
import org.apache.commons.httpclient.util.DateUtil;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class HttpConfigSource implements ConfigSource<InputStream> {
    private final URL sourceUrl;

    public HttpConfigSource(String sourceUrlStr) {
        try {
            this.sourceUrl = new URL(sourceUrlStr);
        } catch (Exception e) {
            throw new RuntimeException("Error while parsing '%s' as a url.".formatted(sourceUrlStr), e);
        }
    }

    @Override
    public Update<InputStream> fetch() throws IOException {
        Optional<Update<InputStream>> fetched = fetchIfNewer(0);
        if (fetched.isPresent()) {
            return fetched.get();
        } else {
            throw new RuntimeException("Unconditional get failed.");
        }
    }

    @Override
    public Optional<Update<InputStream>> fetchIfNewer(long ifNewerThan) throws IOException {
        final HttpURLConnection connection = (HttpURLConnection) sourceUrl.openConnection();
        connection.setIfModifiedSince(ifNewerThan);
        connection.connect();

        int status = connection.getResponseCode();
        if (status == HttpURLConnection.HTTP_OK) {
            return Optional.of(buildUpdate(connection));
        } else if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
            return Optional.empty();
        } else {
            throw new RuntimeException("Unexpected response code: %d when fetching from %s".formatted(status, sourceUrl));
        }
    }

    private Update<InputStream> buildUpdate(HttpURLConnection connection) throws IOException {
        long mtime;
        try {
            String modified = connection.getHeaderField("Last-Modified");
            Date mdate = DateUtil.parseDate(modified);
            mtime = mdate.getTime();
        } catch (Exception e) {
            throw new RuntimeException("Error getting mtime for update.", e);
        }

        return new Update<InputStream>(mtime, getStream(connection));
    }

    private InputStream getStream(HttpURLConnection connection) throws IOException {
        InputStream stream = connection.getInputStream();
        final String contentEncoding = connection.getContentEncoding();
        if (StringUtils.isNotBlank(contentEncoding) && contentEncoding.toLowerCase().contains("gzip")) {
            stream = new GZIPInputStream(stream);
        }
        return stream;
    }

}
