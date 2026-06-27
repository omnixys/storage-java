package com.omnixys.storage.autoconfigure;

import com.omnixys.storage.FileStorageService;
import com.omnixys.storage.S3FileStorageService;
import com.omnixys.storage.health.S3HealthIndicator;
import com.omnixys.storage.model.StorageProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import software.amazon.awssdk.services.s3.S3Client;

@AutoConfiguration
@ConditionalOnClass(S3Client.class)
@ConditionalOnProperty(prefix = "omnixys.storage", name = "bucket")
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public FileStorageService fileStorageService(StorageProperties properties) {
        return new S3FileStorageService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.boot.health.contributor.HealthIndicator")
    public S3HealthIndicator s3HealthIndicator(FileStorageService fileStorageService) {
        return new S3HealthIndicator(fileStorageService);
    }
}
