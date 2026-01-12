package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.UserSearchResultDTO;
import com.my.challenger.dto.user.UpdateUserProfileRequest;
import com.my.challenger.dto.user.UserProfileResponse;
import com.my.challenger.dto.user.UserStatsResponse;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserRelationship;
import com.my.challenger.exception.BadRequestException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.ChallengeProgressRepository;
import com.my.challenger.repository.ChallengeRepository;
import com.my.challenger.repository.UserRelationshipRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final UserRelationshipRepository relationshipRepository;

    /**
     * Search users with enhanced filters and mutual connections count
     */
    @Transactional(readOnly = true)
    public Page<UserSearchResultDTO> searchUsersEnhanced(String searchTerm, Long currentUserId, boolean excludeConnected, int page, int limit) {
        log.debug("Enhanced searching users with term: {} (currentUserId: {}, excludeConnected: {})", 
                searchTerm, currentUserId, excludeConnected);

        if (!StringUtils.hasText(searchTerm)) {
            throw new BadRequestException("Search term cannot be empty");
        }

        if (searchTerm.length() < 2) {
            throw new BadRequestException("Search term must be at least 2 characters long");
        }

        Pageable pageable = PageRequest.of(page, limit);
        Page<User> userPage = userRepository.searchUsers(searchTerm, currentUserId, excludeConnected, pageable);

        return userPage.map(user -> {
            long mutualCount = 0;
            String connectionStatus = "NONE";

            if (currentUserId != null) {
                mutualCount = relationshipRepository.countMutualConnections(currentUserId, user.getId());
                
                var relationship = relationshipRepository.findBetweenUsers(currentUserId, user.getId());
                if (relationship.isPresent()) {
                    UserRelationship rel = relationship.get();
                    if (rel.getStatus().name().equals("ACCEPTED")) {
                        connectionStatus = "CONNECTED";
                    } else if (rel.getStatus().name().equals("PENDING")) {
                        connectionStatus = rel.getUser().getId().equals(currentUserId) ? "PENDING_SENT" : "PENDING_RECEIVED";
                    }
                }
            }

            return UserSearchResultDTO.builder()
                    .id(user.getId())
                    .username(user.getUsername())
                    .avatar(user.getProfilePictureUrl())
                    .bio(user.getBio())
                    .mutualConnectionsCount(mutualCount)
                    .connectionStatus(connectionStatus)
                    .build();
        });
    }

    /**
     * Get user statistics - ENHANCED with better error handling
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(Long userId) {
        log.debug("Fetching user stats for userId: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        try {
            // Get challenge statistics
            long createdChallenges = challengeRepository.countByCreatorId(userId);
            long completedChallenges = challengeProgressRepository.countCompletedChallengesByUserId(userId);

            // Calculate success rate using repository method (now handles division by zero)
            Double successRate = challengeRepository.getSuccessRateByCreatorId(userId);

            // Additional safety check - should not be null anymore, but just in case
            if (successRate == null) {
                log.warn("Success rate calculation returned null for userId: {}. Setting to 0.0", userId);
                successRate = 0.0;
            }

            // Ensure success rate is within valid range (0-100)
            if (successRate < 0.0) {
                log.warn("Invalid success rate {} for userId: {}. Setting to 0.0", successRate, userId);
                successRate = 0.0;
            } else if (successRate > 100.0) {
                log.warn("Success rate {} exceeds 100% for userId: {}. Setting to 100.0", successRate, userId);
                successRate = 100.0;
            }

            UserStatsResponse stats = UserStatsResponse.builder()
                    .created((int) createdChallenges)
                    .completed((int) completedChallenges)
                    .success(Math.round(successRate * 100.0) / 100.0) // Round to 2 decimal places
                    .build();

            log.debug("User stats for userId {}: created={}, completed={}, success={}%",
                    userId, stats.getCreated(), stats.getCompleted(), stats.getSuccess());

            return stats;

        } catch (Exception e) {
            log.error("Error calculating user stats for userId: {}", userId, e);

            // Fallback: return basic stats without success rate calculation
            try {
                long createdChallenges = challengeRepository.countByCreatorId(userId);
                long completedChallenges = challengeProgressRepository.countCompletedChallengesByUserId(userId);

                UserStatsResponse fallbackStats = UserStatsResponse.builder()
                        .created((int) createdChallenges)
                        .completed((int) completedChallenges)
                        .success(0.0) // Safe fallback
                        .build();

                log.warn("Returning fallback stats for userId: {}", userId);
                return fallbackStats;

            } catch (Exception fallbackException) {
                log.error("Even fallback stats calculation failed for userId: {}", userId, fallbackException);

                // Ultimate fallback - return zero stats
                return UserStatsResponse.builder()
                        .created(0)
                        .completed(0)
                        .success(0.0)
                        .build();
            }
        }
    }

    /**
     * Alternative method using detailed stats (if you want more granular data)
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStatsDetailed(Long userId) {
        log.debug("Fetching detailed user stats for userId: {}", userId);

        // Verify user exists
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found with id: " + userId);
        }

        try {
            // Get basic counts
            long completedChallenges = challengeProgressRepository.countCompletedChallengesByUserId(userId);

            // Get detailed success rate stats
            Object[] results = challengeRepository.getDetailedSuccessRateByCreatorId(userId);

            if (results.length >= 5) {
                Long totalChallenges = (Long) results[0];
                Long completedCreated = (Long) results[1];
                Long activeChallenges = (Long) results[2];
                Long failedChallenges = (Long) results[3];
                Double successRate = (Double) results[4];

                log.debug("Detailed stats for userId {}: total={}, completed={}, active={}, failed={}, rate={}%",
                        userId, totalChallenges, completedCreated, activeChallenges, failedChallenges, successRate);

                return UserStatsResponse.builder()
                        .created(totalChallenges.intValue())
                        .completed((int) completedChallenges)
                        .success(successRate != null ? Math.round(successRate * 100.0) / 100.0 : 0.0)
                        .build();
            } else {
                // Fallback to basic method
                return getUserStats(userId);
            }

        } catch (Exception e) {
            log.error("Error in detailed stats calculation for userId: {}, falling back to basic stats", userId, e);
            return getUserStats(userId);
        }
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