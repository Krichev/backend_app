package com.my.challenger.web.controllers;

import com.my.challenger.config.JwtAuthenticationEntryPoint;
import com.my.challenger.config.JwtRequestFilter;
import com.my.challenger.dto.user.*;
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
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "User profile and management operations")
public class UserController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtRequestFilter jwtRequestFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/api/users/profile/**").authenticated()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                );

        http.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
    @PutMapping("/profile/username")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AuthResponse> updateUsername(
            @RequestBody @Valid UpdateUsernameRequest request,
            Authentication authentication) {

        String oldUsername = authentication.getName();

        // Update username in database
        User updatedUser = userService.updateUsername(oldUsername, request.getNewUsername());

        // Generate new JWT token with updated username
        UserDetails userDetails = userDetailsService.loadUserByUsername(updatedUser.getUserName());
        String newToken = jwtTokenProvider.generateToken(userDetails);

        // Create response with new token
        AuthResponse authResponse = AuthResponse.builder()
                .token(newToken)
                .user(mapToUserResponse(updatedUser))
                .message("Username updated successfully")
                .build();

        return ResponseEntity.ok(authResponse);
    }

    @PutMapping("/profile")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestBody @Valid UpdateProfileRequest request,
            Authentication authentication) {

        String username = authentication.getName();
        User updatedUser = userService.updateProfile(username, request);

        // If username was changed, return new token
        if (request.getUserName() != null && !request.getUserName().equals(username)) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(updatedUser.getUserName());
            String newToken = jwtTokenProvider.generateToken(userDetails);

            UserProfileResponse response = mapToUserProfileResponse(updatedUser);
            response.setNewToken(newToken);
            return ResponseEntity.ok(response);
        }

        return ResponseEntity.ok(mapToUserProfileResponse(updatedUser));
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .build();
    }

    private UserProfileResponse mapToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId())
                .userName(user.getUserName())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .build();
    }

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