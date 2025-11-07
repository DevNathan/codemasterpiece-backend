package com.app.codemasterpiecebackend.util;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class CdnProperties {
    @Value("${file.s3.cdnHost}")
    private String baseUrl;
    @Value("${file.s3.keyPrefix}")
    private String keyPrefix;
}
