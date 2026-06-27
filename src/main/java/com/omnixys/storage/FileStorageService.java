package com.omnixys.storage;

import java.io.InputStream;
import java.time.Duration;

public interface FileStorageService {

    String upload(String key, byte[] data, String contentType);

    String uploadStream(String key, InputStream data, String contentType, long contentLength);

    InputStream getStream(String key);

    byte[] get(String key);

    void delete(String key);

    boolean exists(String key);

    String getSignedUploadUrl(String key, String contentType, Duration ttl);

    String getSignedDownloadUrl(String key, Duration ttl);

    String getPublicUrl(String key);

    StorageHealth health();

    record StorageHealth(boolean healthy, String status, Long latencyMs, String error) {}
}
