package com.my.challenger.web.controllers;

import com.my.challenger.dto.user.UpdateUserProfileRequest;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management operations")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;

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

    @PatchMapping("/{userId}")
    public ResponseEntity<UserProfileResponse> updateUserProfile(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserProfileRequest updateRequest,
            @CurrentUser UserPrincipal currentUser) {

        if (!currentUser.getId().equals(userId)) {
            return ResponseEntity.status(403).build();
        }

        return ResponseEntity.ok(userService.updateUserProfile(userId, updateRequest));
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

    @Operation(summary = "Get current user profile")
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getCurrentUserProfile(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getUserFromUserDetails(userDetails);
        log.info("Getting current user profile for userId: {}", currentUser.getId());
        
        try {
            UserProfileResponse userProfile = userService.getUserProfile(currentUser.getId());
            return ResponseEntity.ok(userProfile);
        } catch (Exception e) {
            log.error("Error getting current user profile for userId: {}", currentUser.getId(), e);
            throw e;
        }
    }

    @Operation(summary = "Search users by username")
    @GetMapping("/search")
    public ResponseEntity<?> searchUsers(
            @RequestParam(name = "q", required = true) String searchTerm,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        
        log.info("Searching users with term: {} (limit: {})", searchTerm, limit);
        
        try {
            var users = userService.searchUsers(searchTerm, limit);
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error searching users with term: {}", searchTerm, e);
            throw e;
        }
    }

    @Operation(summary = "Delete user account")
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == #this.getCurrentUserId(#userDetails)")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User currentUser = getUserFromUserDetails(userDetails);
        log.info("Deleting user account for userId: {} by user: {}", userId, currentUser.getId());
        
        // Additional security check
        if (!currentUser.getId().equals(userId)) {
            log.warn("User {} attempted to delete profile of user {}", currentUser.getId(), userId);
            return ResponseEntity.status(403).build(); // Forbidden
        }
        
        try {
            userService.deleteUser(userId);
            log.info("Successfully deleted user account for userId: {}", userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error deleting user account for userId: {}", userId, e);
            throw e;
        }
    }

    // Helper method to get User entity from UserDetails
    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found: " + userDetails.getUsername()));
    }

    // Helper method for SpEL expression in @PreAuthorize
    public Long getCurrentUserId(UserDetails userDetails) {
        User user = getUserFromUserDetails(userDetails);
        return user.getId();
    }
}