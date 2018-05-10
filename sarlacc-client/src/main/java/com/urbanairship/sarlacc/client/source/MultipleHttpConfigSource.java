package com.urbanairship.sarlacc.client.source;

import com.google.common.collect.Lists;
import com.urbanairship.sarlacc.client.model.Update;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class MultipleHttpConfigSource implements ConfigSource<InputStream> {

    private static final Logger log = LogManager.getLogger(MultipleHttpConfigSource.class);
    private final List<HttpConfigSource> httpConfigSources;

    public MultipleHttpConfigSource(List<String> sourceUrls) {
        httpConfigSources = Lists.newArrayList();
        for (String sourceUrl : sourceUrls) {
            httpConfigSources.add(new HttpConfigSource(sourceUrl));
        }
    }

    @Override
    public Optional<Update<InputStream>> fetchIfNewer(long ifNewerThan) throws IOException {
        for (HttpConfigSource httpConfigSource : httpConfigSources) {
            try {
                return httpConfigSource.fetchIfNewer(ifNewerThan);
            } catch (Exception e) {
                log.error(e);
            }
        }
        throw new IOException("All HTTP sources failed.");
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
}
