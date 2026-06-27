# Omnixys Storage

S3-compatible file storage abstraction for Spring Boot.

## Features

- S3 file storage service (upload, download, delete, list)
- Storage exception model
- Spring Boot auto-configuration

## Installation

```xml
<dependency>
    <groupId>com.omnixys</groupId>
    <artifactId>storage</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

```java
@Autowired
private FileStorageService storageService;

storageService.upload("bucket", "key", inputStream);
InputStream data = storageService.download("bucket", "key");
```
