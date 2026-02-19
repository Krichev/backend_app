package com.my.challenger.web.controllers;

import com.my.challenger.dto.appversion.AppVersionCheckResponse;
import com.my.challenger.dto.appversion.UpdateAppVersionConfigRequest;
import com.my.challenger.entity.AppVersionConfig;
import com.my.challenger.service.impl.AppVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@Tag(name = "App Version", description = "In-app update version checking")
public class AppVersionController {

    private final AppVersionService appVersionService;

    /**
     * PUBLIC endpoint — no auth required.
     * Mapped under /public/ which is already whitelisted in WebSecurityConfig.
     */
    @GetMapping("/public/app-version/check")
    @Operation(summary = "Check for app updates",
               description = "Public endpoint. Compares client version against latest GitHub release.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Version check result"),
        @ApiResponse(responseCode = "503", description = "GitHub API unavailable")
    })
    public ResponseEntity<AppVersionCheckResponse> checkForUpdate(
            @RequestParam(defaultValue = "android") String platform,
            @RequestParam String currentVersion) {
        log.info("GET /public/app-version/check - platform: {}, currentVersion: {}", platform, currentVersion);
        try {
            return ResponseEntity.ok(appVersionService.checkForUpdate(platform, currentVersion));
        } catch (Exception e) {
            log.error("Failed to check for updates", e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }

    /**
     * ADMIN endpoint — authenticated.
     * For updating min version / force update config.
     */
    @PutMapping("/api/app-version/config")
    @Operation(summary = "Update app version config (admin)")
    public ResponseEntity<AppVersionConfig> updateConfig(
            @RequestParam(defaultValue = "android") String platform,
            @Valid @RequestBody UpdateAppVersionConfigRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("PUT /api/app-version/config - platform: {}, by: {}", platform, userDetails.getUsername());
        return ResponseEntity.ok(appVersionService.updateConfig(platform, request));
    }
}
