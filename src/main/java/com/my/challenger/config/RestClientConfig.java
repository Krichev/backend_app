package com.my.challenger.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Slf4j
@Configuration
public class RestClientConfig {

    @Value("${rest.client.connect-timeout:10}")
    private int connectTimeout;

    @Value("${rest.client.read-timeout:30}")
    private int readTimeout;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
            .setConnectTimeout(Duration.ofSeconds(connectTimeout))
            .setReadTimeout(Duration.ofSeconds(readTimeout))
            .interceptors(loggingInterceptor())
            .build();
    }

    private ClientHttpRequestInterceptor loggingInterceptor() {
        return (request, body, execution) -> {
            log.debug("🌐 External call: {} {}", request.getMethod(), request.getURI());
            var response = execution.execute(request, body);
            log.debug("📥 Response status: {}", response.getStatusCode());
            return response;
        };
    }
}
