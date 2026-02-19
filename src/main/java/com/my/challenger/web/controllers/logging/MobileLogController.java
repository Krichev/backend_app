package com.my.challenger.web.controllers.logging;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.my.challenger.dto.logging.MobileLogBatchRequest;
import com.my.challenger.service.logging.MobileLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
@RequestMapping("/public/logs")
public class MobileLogController {

    private final MobileLogService mobileLogService;
    private final Cache<String, AtomicInteger> rateLimitCache;

    public MobileLogController(MobileLogService mobileLogService) {
        this.mobileLogService = mobileLogService;
        this.rateLimitCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build();
    }

    @PostMapping("/batch")
    public ResponseEntity<?> receiveLogs(@Valid @RequestBody MobileLogBatchRequest request, HttpServletRequest servletRequest) {
        String clientIp = servletRequest.getRemoteAddr();
        
        // Simple Rate Limiting: max 100 requests per minute per IP
        AtomicInteger count = rateLimitCache.get(clientIp, k -> new AtomicInteger(0));
        if (count.incrementAndGet() > 100) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("error", "Rate limit exceeded. Max 100 requests per minute."));
        }

        log.debug("Received log batch for session: {}, entries: {}", 
                request.getSessionId(), request.getLogs().size());
        
        mobileLogService.writeLogs(request.getSessionId(), request.getDeviceInfo(), request.getLogs());
        
        return ResponseEntity.ok(Map.of(
            "received", request.getLogs().size(),
            "status", "success"
        ));
    }
}
