package org.hiten.blogspherebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "r2")
public class R2StorageConfig {

    private String accountId;

    private String accessKey;

    private String secretKey;

    private String bucket;

    private String publicUrl;
}