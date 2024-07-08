package org.yulia.filemanagement.fileuploadservice.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    // Minio server URL
    @Value("${minio.url}")
    private String minioUrl;

    // Access key for Minio
    @Value("${minio.access-key}")
    private String accessKey;

    // Secret key for Minio
    @Value("${minio.secret-key}")
    private String secretKey;

    /**
     * Configures and returns a MinioClient bean.
     *
     * @return a configured MinioClient instance
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioUrl)
                .credentials(accessKey, secretKey)
                .build();
    }
}

