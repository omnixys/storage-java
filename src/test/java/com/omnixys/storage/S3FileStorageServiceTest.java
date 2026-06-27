package com.omnixys.storage;

import com.omnixys.storage.model.StorageProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class S3FileStorageServiceTest {

    @Test
    void shouldRejectMissingBucket() {
        var props = new StorageProperties();
        assertNull(props.getBucket());
    }

    @Test
    void shouldConfigureProperties() {
        var props = new StorageProperties();
        props.setEndpoint("http://localhost:9000");
        props.setBucket("test-bucket");
        props.setRegion("us-east-1");
        props.setAccessKeyId("minioadmin");
        props.setSecretAccessKey("minioadmin");

        assertEquals("http://localhost:9000", props.getEndpoint());
        assertEquals("test-bucket", props.getBucket());
        assertTrue(props.isForcePathStyle());
        assertEquals(3600, props.getLinkTTL());
    }
}
