package com.urbanairship.sarlacc.client.source;

import com.google.common.collect.Lists;
import com.urbanairship.sarlacc.client.model.Update;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

public class MultipleHttpSource implements ConfigSource<InputStream> {

    private static final Logger log = LogManager.getLogger(MultipleHttpSource.class);
    private final List<HttpSource> httpSources;

    public MultipleHttpSource(List<String> sourceUrls) {
        httpSources = Lists.newArrayList();
        for (String sourceUrl : sourceUrls) {
            httpSources.add(new HttpSource(sourceUrl));
        }
    }

    @Override
    public Optional<Update<InputStream>> fetchIfNewer(long ifNewerThan) throws IOException {
        for (HttpSource httpSource : httpSources) {
            try {
                return httpSource.fetchIfNewer(ifNewerThan);
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
