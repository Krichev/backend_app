package com.my.challenger.service.impl;

import com.my.challenger.dto.user.UpdateProfileRequest;
import com.my.challenger.dto.user.UpdateUserProfileRequest;
import com.my.challenger.dto.user.UserProfileResponse;
import com.my.challenger.dto.user.UserStatsResponse;
import com.my.challenger.entity.User;
import com.my.challenger.exception.BadRequestException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.ChallengeProgressRepository;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final ChallengeProgressRepository challengeProgressRepository;
    private final PasswordEncoder passwordEncoder;

    public User updateUsername(String oldUsername, String newUsername) {
        // Check if new username already exists
        if (userRepository.existsByUserName(newUsername)) {
            throw new BadRequestException("Username already taken");
        }

        User user = userRepository.findByUserName(oldUsername)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        user.setUserName(newUsername);
        return userRepository.save(user);
    }

    public User updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Update username if provided and different
        if (request.getUserName() != null && !request.getUserName().equals(username)) {
            if (userRepository.existsByUserName(request.getUserName())) {
                throw new BadRequestException("Username already taken");
            }
            user.setUserName(request.getUserName());
        }

        // Update other fields if provided
        if (request.getEmail() != null) {
            if (!request.getEmail().equals(user.getEmail()) &&
                    userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Email already in use");
            }
            user.setEmail(request.getEmail());
        }

        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }

        if (request.getPhoneNumber() != null) {
            user.setPhoneNumber(request.getPhoneNumber());
        }

        if (request.getAddress() != null) {
            user.setAddress(request.getAddress());
        }

        return userRepository.save(user);
    }
    /**
     * Get user profile by ID
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(Long userId) {
        log.debug("Fetching user profile for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        return convertToUserProfileResponse(user);
    }

    /**
     * Update user profile
     */
    public UserProfileResponse updateUserProfile(Long userId, UpdateUserProfileRequest updateRequest) {
        log.debug("Updating user profile for userId: {} with data: {}", userId, updateRequest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Validate username uniqueness if username is being changed
        if (StringUtils.hasText(updateRequest.getUsername()) &&
                !updateRequest.getUsername().equals(user.getUsername())) {

            if (userRepository.existsByUsername(updateRequest.getUsername())) {
                throw new BadRequestException("Username '" + updateRequest.getUsername() + "' is already taken");
            }

            // Validate username format
            if (!isValidUsername(updateRequest.getUsername())) {
                throw new BadRequestException("Username must be between 3-50 characters and contain only letters, numbers, and underscores");
            }

            user.setUsername(updateRequest.getUsername());
            log.debug("Updated username for userId: {} to: {}", userId, updateRequest.getUsername());
        }

        // Update bio
        if (updateRequest.getBio() != null) {
            // Allow empty string to clear bio
            if (updateRequest.getBio().length() > 500) {
                throw new BadRequestException("Bio cannot exceed 500 characters");
            }
            user.setBio(updateRequest.getBio().trim());
            log.debug("Updated bio for userId: {}", userId);
        }

        // Update avatar
        if (updateRequest.getAvatar() != null) {
            // Validate avatar URL format if needed
            if (StringUtils.hasText(updateRequest.getAvatar()) && !isValidAvatarUrl(updateRequest.getAvatar())) {
                throw new BadRequestException("Invalid avatar URL format");
            }
            user.setProfilePictureUrl(updateRequest.getAvatar());
            log.debug("Updated avatar for userId: {}", userId);
        }

        // Update timestamp
        user.setUpdatedAt(LocalDateTime.now());

        // Save the updated user
        User savedUser = userRepository.save(user);
        log.info("Successfully updated user profile for userId: {}", userId);

        return convertToUserProfileResponse(savedUser);
    }

    /**
     * Get user statistics
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        log.debug("Fetching user stats for userId: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        // Get challenge statistics
        long createdChallenges = challengeRepository.countByCreatorId(userId);
        long completedChallenges = challengeProgressRepository.countCompletedChallengesByUserId(userId);

        // Calculate success rate using repository method (more accurate for creator success rate)
        Double successRate = challengeRepository.getSuccessRateByCreatorId(userId);
        if (successRate == null) {
            successRate = 0.0;
        }

        UserStatsResponse stats = UserStatsResponse.builder()
                .created((int) createdChallenges)
                .completed((int) completedChallenges)
                .success(successRate)
                .build();

        log.debug("User stats for userId {}: {}", userId, stats);
        return stats;
    }

    /**
     * Search users by username
     */
    @Transactional(readOnly = true)
    public List<UserProfileResponse> searchUsers(String searchTerm, int limit) {
        log.debug("Searching users with term: {} (limit: {})", searchTerm, limit);

        if (!StringUtils.hasText(searchTerm)) {
            throw new BadRequestException("Search term cannot be empty");
        }

        if (searchTerm.length() < 2) {
            throw new BadRequestException("Search term must be at least 2 characters long");
        }

        List<User> users = userRepository.findByUsernameContainingIgnoreCase(searchTerm)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());

        return users.stream()
                .map(this::convertToUserProfileResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete user account
     */
    public void deleteUser(Long userId) {
        log.debug("Deleting user account for userId: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Note: In a production system, you might want to soft delete or anonymize
        // the user data instead of hard delete to maintain referential integrity
        userRepository.delete(user);

        log.info("Successfully deleted user account for userId: {}", userId);
    }

    /**
     * Convert User entity to UserProfileResponse DTO
     */
    private UserProfileResponse convertToUserProfileResponse(User user) {
        return UserProfileResponse.builder()
                .id(user.getId().toString())
                .username(user.getUsername())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatar(user.getProfilePictureUrl())
                .createdAt(user.getCreatedAt().toString())
                .build();
    }

    /**
     * Validate username format
     */
    private boolean isValidUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return false;
        }

        // Username must be 3-50 characters, containing only letters, numbers, and underscores
        return username.length() >= 3 &&
                username.length() <= 50 &&
                username.matches("^[a-zA-Z0-9_]+$");
    }

    /**
     * Validate avatar URL format
     */
    private boolean isValidAvatarUrl(String avatarUrl) {
        if (!StringUtils.hasText(avatarUrl)) {
            return true; // Empty URL is valid (will clear avatar)
        }

        // Basic URL validation - you might want to make this more sophisticated
        return avatarUrl.startsWith("http://") ||
                avatarUrl.startsWith("https://") ||
                avatarUrl.startsWith("file://") ||
                avatarUrl.startsWith("content://"); // For Android content URIs
    }
}