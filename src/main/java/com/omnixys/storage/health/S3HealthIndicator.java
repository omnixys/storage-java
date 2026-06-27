package com.omnixys.storage.health;

import com.omnixys.storage.FileStorageService;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

public class S3HealthIndicator implements HealthIndicator {

    private final FileStorageService storageService;

    public S3HealthIndicator(FileStorageService storageService) {
        this.storageService = storageService;
    }

    @Override
    public Health health() {
        try {
            FileStorageService.StorageHealth sh = storageService.health();
            if (sh.healthy()) {
                return Health.up()
                        .withDetail("status", sh.status())
                        .withDetail("latencyMs", sh.latencyMs())
                        .build();
            }
            return Health.down()
                    .withDetail("status", sh.status())
                    .withDetail("error", sh.error())
                    .build();
        } catch (Exception e) {
            return Health.down(e).build();
        }
    }
}
