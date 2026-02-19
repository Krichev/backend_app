package com.my.challenger.web.controllers;

import com.my.challenger.dto.settings.UpdateAppSettingsRequest;
import com.my.challenger.dto.settings.UserAppSettingsDTO;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.UserAppSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/app-settings")
@RequiredArgsConstructor
@Tag(name = "App Settings", description = "Manage user application settings (language, theme, notifications)")
public class UserAppSettingsController {

    private final UserAppSettingsService settingsService;

    /**
     * Get app settings for current user (auto-creates default if not exists)
     */
    @GetMapping
    @Operation(summary = "Get app settings for current user",
               description = "Returns user's application settings. Creates default settings if none exist.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings retrieved successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserAppSettingsDTO> getMySettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((UserPrincipal) userDetails).getId();
        log.debug("GET /api/app-settings - userId: {}", userId);
        return ResponseEntity.ok(settingsService.getOrCreateSettings(userId));
    }

    /**
     * Update app settings (partial update)
     */
    @PutMapping
    @Operation(summary = "Update app settings",
               description = "Partially updates user's application settings. Only provided fields are updated.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Settings updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request body"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserAppSettingsDTO> updateSettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateAppSettingsRequest request) {
        Long userId = ((UserPrincipal) userDetails).getId();
        log.debug("PUT /api/app-settings - userId: {}, request: {}", userId, request);
        return ResponseEntity.ok(settingsService.updateSettings(userId, request));
    }

    /**
     * Quick endpoint to update only language
     */
    @PatchMapping("/language")
    @Operation(summary = "Update language preference only",
               description = "Quick endpoint to update just the language setting.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Language updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid language code"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<UserAppSettingsDTO> updateLanguage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Parameter(description = "Language code: 'en' or 'ru'", required = true)
            @RequestParam String language) {
        Long userId = ((UserPrincipal) userDetails).getId();
        log.debug("PATCH /api/app-settings/language - userId: {}, language: {}", userId, language);
        
        if (!language.matches("^(en|ru)$")) {
            log.warn("Invalid language code: {}", language);
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(settingsService.updateLanguage(userId, language));
    }
}
