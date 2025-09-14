package com.my.challenger.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Simple metrics configuration
 * Only creates a MeterRegistry if metrics are enabled and none exists
 */
@Configuration
@ConditionalOnProperty(name = "app.metrics.enabled", havingValue = "true")
public class SimpleMetricsConfig {

    /**
     * Create a simple in-memory meter registry as fallback
     * Spring Boot Actuator will override this with its own if present
     */
    @Bean
    @ConditionalOnMissingBean(MeterRegistry.class)
    public MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }
}