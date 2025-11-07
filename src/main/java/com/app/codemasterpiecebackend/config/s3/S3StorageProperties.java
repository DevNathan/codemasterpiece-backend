package com.app.codemasterpiecebackend.config.s3;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "file.s3")
public class S3StorageProperties {
    private String bucket;
    private String region;
    private String accessKeyId;
    private String secretAccessKey;
    private String sessionToken;
    private String endpoint;
    private boolean pathStyle;
    private Integer connectTimeoutMs = 3000;
    private Integer readTimeoutMs = 10000;
    private Integer maxRetries = 3;
    private String keyPrefix = "";
}
