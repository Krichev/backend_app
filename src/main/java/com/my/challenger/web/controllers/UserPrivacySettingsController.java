package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.UpdatePrivacySettingsRequest;
import com.my.challenger.dto.quiz.UserPrivacySettingsDTO;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.UserPrivacySettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/privacy-settings")
@RequiredArgsConstructor
@Tag(name = "Privacy Settings", description = "Manage user privacy settings")
public class UserPrivacySettingsController {

    private final UserPrivacySettingsService privacyService;

    @GetMapping
    @Operation(summary = "Get privacy settings for current user")
    public ResponseEntity<UserPrivacySettingsDTO> getMyPrivacySettings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(privacyService.getPrivacySettings(userId));
    }

    @PutMapping
    @Operation(summary = "Update privacy settings")
    public ResponseEntity<UserPrivacySettingsDTO> updatePrivacySettings(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestBody UpdatePrivacySettingsRequest request) {
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(privacyService.updatePrivacySettings(userId, request));
    }
}
