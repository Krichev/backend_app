// src/main/java/com/my/challenger/web/controllers/UserController.java
package com.my.challenger.web.controllers;

import com.my.challenger.config.JwtTokenUtil;
import com.my.challenger.dto.quiz.UserSearchResultDTO;
import com.my.challenger.dto.user.UpdateUserProfileRequest;
import com.my.challenger.dto.user.UpdateUserProfileResponse;
import com.my.challenger.dto.user.UserProfileResponse;
import com.my.challenger.dto.user.UserStatsResponse;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.security.CurrentUser;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management operations")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenUtil jwtTokenUtil;

    @Operation(summary = "Get user profile by ID")
    @GetMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable Long userId) {
        log.info("Getting user profile for userId: {}", userId);

        try {
            UserProfileResponse userProfile = userService.getUserProfile(userId);
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            log.error("Error getting user profile for userId: {}", userId, e);
            throw e;
        }
    }

    @Operation(summary = "Update user profile")
    @PatchMapping("/{userId}")
    public ResponseEntity<UpdateUserProfileResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserProfileRequest updateRequest,
            @CurrentUser UserPrincipal currentUser) {

        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        // Get the original username before update
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        String originalUsername = user.getUsername();

        // Update the user profile
        UserProfileResponse updatedProfile = userService.updateUserProfile(userId, updateRequest);

        // Check if username was changed
        boolean usernameChanged = updateRequest.getUsername() != null &&
                !updateRequest.getUsername().equals(originalUsername);

        UpdateUserProfileResponse response;

        if (usernameChanged) {
            // Generate new JWT token with the new username
            String newToken = jwtTokenUtil.generateToken(updateRequest.getUsername());
            response = new UpdateUserProfileResponse(updatedProfile, newToken);

            log.info("Username updated from '{}' to '{}' for userId: {}. New token generated.",
                    originalUsername, updateRequest.getUsername(), userId);
        } else {
            // No username change, just return updated profile
            response = new UpdateUserProfileResponse(updatedProfile);
            log.info("Profile updated for userId: {} (no username change)", userId);
        }

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Get user statistics")
    @GetMapping("/{userId}/stats")
    public ResponseEntity<UserStatsResponse> getUserStats(@PathVariable Long userId) {
        log.info("Getting user stats for userId: {}", userId);

        try {
            UserStatsResponse stats = userService.getUserStats(userId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("Error getting user stats for userId: {}", userId, e);
            throw e;
        }
    }


    @Operation(summary = "Search users with enhanced filters")
    @GetMapping("/search")
    public ResponseEntity<Page<UserSearchResultDTO>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "false") boolean excludeConnected,
            @CurrentUser UserPrincipal currentUser) {

        try {
            Long currentUserId = currentUser != null ? currentUser.getId() : null;
            return ResponseEntity.ok(userService.searchUsersEnhanced(q, currentUserId, excludeConnected, page, limit));
        } catch (Exception e) {
            log.error("Error searching users with query: {}", q, e);
            throw e;
        }
    }

    @Operation(summary = "Get current user profile")
    @GetMapping("/profile")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(@CurrentUser UserPrincipal currentUser) {
        try {
            UserProfileResponse userProfile = userService.getUserProfile(currentUser.getId());
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            log.error("Error getting current user profile for userId: {}", currentUser.getId(), e);
            throw e;
        }
    }
}