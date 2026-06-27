package com.omnixys.storage;

import com.omnixys.storage.model.StorageProperties;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

public class S3FileStorageService implements FileStorageService {

    private final S3Client client;
    private final S3Presigner presigner;
    private final StorageProperties properties;
    private final AtomicInteger activeOperations = new AtomicInteger(0);
    private volatile boolean closed = false;

    public S3FileStorageService(StorageProperties properties) {
        this.properties = properties;
        var builder = S3Client.builder()
                .region(software.amazon.awssdk.regions.Region.of(properties.getRegion()))
                .forcePathStyle(properties.isForcePathStyle());

        if (properties.getEndpoint() != null && !properties.getEndpoint().isBlank()) {
            builder.endpointOverride(URI.create(properties.getEndpoint()));
        }

        if (properties.getAccessKeyId() != null && !properties.getAccessKeyId().isBlank()) {
            builder.credentialsProvider(() ->
                    software.amazon.awssdk.auth.credentials.AwsBasicCredentials.create(
                            properties.getAccessKeyId(), properties.getSecretAccessKey()));
        }

        this.client = builder.build();
        this.presigner = S3Presigner.builder()
                .region(software.amazon.awssdk.regions.Region.of(properties.getRegion()))
                .endpointOverride(properties.getEndpoint() != null
                        ? URI.create(properties.getEndpoint()) : null)
                .build();
    }

    @Override
    public String upload(String key, byte[] data, String contentType) {
        activeOperations.incrementAndGet();
        try {
            client.putObject(PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .build(), RequestBody.fromBytes(data));
            return getPublicUrl(key);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    @Override
    public String uploadStream(String key, InputStream data, String contentType, long contentLength) {
        activeOperations.incrementAndGet();
        try {
            client.putObject(PutObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .contentType(contentType)
                    .contentLength(contentLength)
                    .build(), RequestBody.fromInputStream(data, contentLength));
            return getPublicUrl(key);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    @Override
    public InputStream getStream(String key) {
        activeOperations.incrementAndGet();
        try {
            ResponseInputStream<GetObjectResponse> response = client.getObject(
                    GetObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .build());
            return response;
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    @Override
    public byte[] get(String key) {
        activeOperations.incrementAndGet();
        try {
            ResponseInputStream<GetObjectResponse> response = client.getObject(
                    GetObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .build());
            return response.readAllBytes();
        } catch (Exception e) {
            throw new StorageException("STORAGE_GET_FAILED", "Failed to get object: " + key, e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    @Override
    public void delete(String key) {
        activeOperations.incrementAndGet();
        try {
            client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
        } catch (Exception e) {
            throw new StorageException("STORAGE_DELETE_FAILED", "Failed to delete object: " + key, e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    @Override
    public boolean exists(String key) {
        activeOperations.incrementAndGet();
        try {
            client.headObject(HeadObjectRequest.builder()
                    .bucket(properties.getBucket())
                    .key(key)
                    .build());
            return true;
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            throw new StorageException("STORAGE_EXISTS_FAILED", "Failed to check object: " + key, e);
        } finally {
            activeOperations.decrementAndGet();
        }
    }

    @Override
    public String getSignedUploadUrl(String key, String contentType, Duration ttl) {
        var presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .putObjectRequest(PutObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .contentType(contentType)
                        .build())
                .build();
        return presigner.presignPutObject(presignRequest).url().toString();
    }

    @Override
    public String getSignedDownloadUrl(String key, Duration ttl) {
        var presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(ttl)
                .getObjectRequest(GetObjectRequest.builder()
                        .bucket(properties.getBucket())
                        .key(key)
                        .build())
                .build();
        return presigner.presignGetObject(presignRequest).url().toString();
    }

    @Override
    public String getPublicUrl(String key) {
        String base = properties.getPublicUrl() != null
                ? properties.getPublicUrl()
                : properties.getEndpoint();
        if (base == null) {
            return properties.getBucket() + "/" + key;
        }
        base = base.replaceAll("/+$", "");
        return base + "/" + properties.getBucket() + "/" + key;
    }

    @Override
    public StorageHealth health() {
        if (closed) return new StorageHealth(false, "closed", null, null);
        long start = System.currentTimeMillis();
        try {
            client.headBucket(HeadBucketRequest.builder()
                    .bucket(properties.getBucket())
                    .build());
            return new StorageHealth(true, "ready", System.currentTimeMillis() - start, null);
        } catch (Exception e) {
            return new StorageHealth(false, "unavailable",
                    System.currentTimeMillis() - start, e.getMessage());
        }
    }

    @PreDestroy
    public void close() {
        closed = true;
        presigner.close();
        client.close();
    }

    public int activeOperations() {
        return activeOperations.get();
    }
}
