package com.my.challenger.service.impl;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PhotoMetricsService {
    
    private final MeterRegistry meterRegistry;
    
    private final Counter uploadCounter = Counter.builder("photo.uploads.total")
            .description("Total number of photo uploads")
            .register(meterRegistry);
    
    private final Timer uploadTimer = Timer.builder("photo.upload.duration")
            .description("Photo upload duration")
            .register(meterRegistry);
    
    private final Counter storageCounter = Counter.builder("photo.storage.bytes")
            .description("Total storage used")
            .register(meterRegistry);
    
    public void recordUpload(long bytes, long durationMillis) {
        uploadCounter.increment();
        uploadTimer.record(durationMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        storageCounter.increment(bytes);
    }
    
    public void recordDeletion(long bytes) {
        storageCounter.increment(-bytes);
    }
}