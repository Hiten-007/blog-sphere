package org.hiten.blogspherebackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "file")
@Getter
@Setter
public class FileStorageConfig {

    private String uploadDir = "uploads";

    private long maxSize = 5_242_880L;

    private List<String> allowedTypes = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );
}
