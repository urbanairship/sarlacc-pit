package com.urbanairship.sarlacc.client.source;

import com.urbanairship.sarlacc.client.model.Update;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

public class FileConfigSource implements ConfigSource<InputStream> {
    private final String filePath;
    private final boolean gzipped;

    public FileConfigSource(String filePath, boolean gzipped) {
        this.filePath = filePath;
        this.gzipped = gzipped;
    }

    @Override
    public Optional<Update<InputStream>> fetchIfNewer(long ifNewerThan) throws IOException {
        final File file = getFile();
        long modified = file.lastModified();
        if (ifNewerThan < modified) {
            return Optional.of(new Update<>(modified, getInputStream(file)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Update<InputStream> fetch() throws IOException {
        final File file = getFile();
        return new Update<>(file.lastModified(), getInputStream(file));
    }

    private File getFile() throws IOException {
        File file = new File(filePath);
        if (file.canRead()) {
            return file;
        } else {
            throw new IOException(String.format(
                    "Couldn't access '%s' Exists: %b Readable: %b", filePath, file.exists(), file.canRead()));
        }
    }

    private InputStream getInputStream(File file) throws IOException {
        InputStream inputStream = new FileInputStream(file);
        if (gzipped) {
            inputStream = new GZIPInputStream(inputStream);
        }
        return inputStream;
    }
}
