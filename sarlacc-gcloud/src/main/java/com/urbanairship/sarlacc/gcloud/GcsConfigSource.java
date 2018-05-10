package com.urbanairship.sarlacc.gcloud;

import com.google.api.client.http.HttpStatusCodes;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Blob.BlobSourceOption;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;
import com.google.cloud.storage.StorageOptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.urbanairship.sarlacc.client.model.Update;
import com.urbanairship.sarlacc.client.source.ConfigSource;
import sun.nio.ch.ChannelInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;

/**
 * Config source backed by an object in Google Cloud Storage
 *
 * Most use cases can build an instance with the basicGcsSource(String bucket, String object) utility function,
 * only those requiring non-default credentials or transport options need to use the full builder.
 *
 * If using the default credentials, the usual GOOGLE_APPLICATION_CREDENTIALS environment variable pointed at
 * a credential file must be present.
 */
public class GcsConfigSource implements ConfigSource<InputStream> {
    private final BlobId sourceBlob;
    private final Storage gcsClient;
    private final BlobSourceOption[] blobSourceOptions;

    private GcsConfigSource(BlobId sourceBlob, Storage gcsClient, BlobSourceOption[] blobSourceOptions) {
        this.sourceBlob = sourceBlob;
        this.gcsClient = gcsClient;
        this.blobSourceOptions = blobSourceOptions;
    }

    @Override
    public Optional<Update<InputStream>> fetchIfNewer(long ifNewerThan) throws IOException {
        final Storage.BlobGetOption getIfDifferentGeneration = Storage.BlobGetOption.generationNotMatch(ifNewerThan);

        final Blob blob;
        try {
            blob = gcsClient.get(sourceBlob, getIfDifferentGeneration);
        } catch (StorageException se) {
            if (se.getCode() == HttpStatusCodes.STATUS_CODE_NOT_MODIFIED) {
                return Optional.empty();
            }

            throw new IOException(se);
        }

        //Instead of a 404, get() returns null if no object is present. Neat.
        if (blob == null) {
            throw new IOException(String.format("Couldn't find object %s in bucket %s", sourceBlob.getName(), sourceBlob.getBucket()));
        }

        if (blob.getGeneration() < ifNewerThan) {
            return Optional.empty();
        }

        final ReadChannel reader = blob.reader(blobSourceOptions);
        final long generation = blob.getBlobId().getGeneration();

        return Optional.of(new Update<>(generation, new ChannelInputStream(reader)));
    }

    @Override
    public Update<InputStream> fetch() throws IOException {
        final Blob blob;
        try {
            blob = gcsClient.get(sourceBlob);
        } catch (StorageException se) {
            throw new IOException(se);
        }

        if (blob == null) {
            throw new IOException("No result found for blob: " + sourceBlob);
        }

        final ReadChannel reader = blob.reader(blobSourceOptions);
        final long generation = blob.getBlobId().getGeneration();

        return new Update<>(generation, new ChannelInputStream(reader));
    }

    public static GcsConfigSource basicGcsSource(String bucket, String object) {
        return builder()
                .setGcsClient(StorageOptions.getDefaultInstance().getService())
                .setSourceBlob(BlobId.of(bucket, object))
                .build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ArrayList<BlobSourceOption> blobSourceOptions = Lists.newArrayList();

        private Storage gcsClient;
        private BlobId sourceBlob;

        public Builder setSourceBlob(final BlobId sourceBlob) {
            this.sourceBlob = sourceBlob;
            return this;
        }

        public Builder setGcsClient(final Storage gcsClient) {
            this.gcsClient = gcsClient;
            return this;
        }

        public Builder addBlobSourceOption(final BlobSourceOption blobSourceOption) {
            this.blobSourceOptions.add(blobSourceOption);
            return this;
        }

        public GcsConfigSource build() {
            Preconditions.checkNotNull(sourceBlob, "sourceBlob must not be null");
            Preconditions.checkNotNull(gcsClient, "gcsClient must not be null");

            final BlobSourceOption[] blobSourceOptions =
                    this.blobSourceOptions.toArray(new BlobSourceOption[this.blobSourceOptions.size()]);

            return new GcsConfigSource(this.sourceBlob, this.gcsClient, blobSourceOptions);
        }
    }
}
