// File: S3StartupConfig.java
package com.my.challenger.config;

import com.my.challenger.service.impl.AdvancedS3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
@RequiredArgsConstructor
@Slf4j
public class S3StartupConfig {

    private final AdvancedS3Service advancedS3Service;

    @Bean
    public ApplicationRunner initializeS3() {
        return args -> {
            try {
                log.info("Initializing S3 bucket on startup...");
                advancedS3Service.createBucketIfNotExists();
                log.info("S3 initialization completed successfully");
            } catch (Exception e) {
                log.warn("S3 initialization failed, but application will continue: {}", e.getMessage());
            }
        };
    }
}

