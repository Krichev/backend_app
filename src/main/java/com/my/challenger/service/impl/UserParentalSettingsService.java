package com.my.challenger.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.my.challenger.dto.parental.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserParentalSettings;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserParentalSettingsRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserParentalSettingsService {

    private final UserParentalSettingsRepository parentalRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    /**
     * Get parental settings for a user, creating defaults if not exists
     */
    @Transactional
    public UserParentalSettingsDTO getOrCreateSettings(Long userId) {
        log.debug("Getting parental settings for user {}", userId);
        UserParentalSettings settings = parentalRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));
        return toDTO(settings);
    }

    /**
     * Update parental settings (partial update)
     */
    @Transactional
    public UserParentalSettingsDTO updateSettings(Long userId, UpdateParentalSettingsRequest request) {
        log.info("Updating parental settings for user {}", userId);
        
        UserParentalSettings settings = parentalRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));

        // Only allow non-child accounts to modify their own settings freely
        // Child accounts need parent verification
        if (Boolean.TRUE.equals(settings.getIsChildAccount()) && settings.getParentUserId() != null) {
            throw new IllegalStateException("Child accounts cannot modify parental settings directly");
        }

        if (request.getAgeGroup() != null) {
            settings.setAgeGroup(request.getAgeGroup());
        }
        if (request.getContentRestrictionLevel() != null) {
            settings.setContentRestrictionLevel(request.getContentRestrictionLevel());
        }
        if (request.getRequireParentApproval() != null) {
            settings.setRequireParentApproval(request.getRequireParentApproval());
        }
        if (request.getAllowedTopicCategories() != null) {
            settings.setAllowedTopicCategories(toJson(request.getAllowedTopicCategories()));
        }
        if (request.getBlockedTopicCategories() != null) {
            settings.setBlockedTopicCategories(toJson(request.getBlockedTopicCategories()));
        }
        if (request.getMaxDailyScreenTimeMinutes() != null) {
            settings.setMaxDailyScreenTimeMinutes(request.getMaxDailyScreenTimeMinutes());
        }
        if (request.getMaxDailyQuizCount() != null) {
            settings.setMaxDailyQuizCount(request.getMaxDailyQuizCount());
        }
        if (StringUtils.hasText(request.getNewParentPin())) {
            validatePin(request.getNewParentPin());
            settings.setParentPinHash(passwordEncoder.encode(request.getNewParentPin()));
        }

        settings = parentalRepository.save(settings);
        log.info("Updated parental settings for user {}", userId);
        return toDTO(settings);
    }

    /**
     * Link a child account to parent
     */
    @Transactional
    public ChildAccountDTO linkChildAccount(Long parentUserId, LinkChildRequest request) {
        log.info("Parent {} linking child account {}", parentUserId, request.getChildUserId());

        // Validate parent exists
        User parent = userRepository.findById(parentUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Parent user not found: " + parentUserId));

        // Validate child exists
        User child = userRepository.findById(request.getChildUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Child user not found: " + request.getChildUserId()));

        // Prevent self-linking
        if (parentUserId.equals(request.getChildUserId())) {
            throw new IllegalArgumentException("Cannot link account to itself");
        }

        // Get or create child's parental settings
        UserParentalSettings childSettings = parentalRepository.findByUserId(request.getChildUserId())
            .orElseGet(() -> createDefaultSettings(request.getChildUserId()));

        // Check if already linked to another parent
        if (childSettings.getParentUserId() != null && !childSettings.getParentUserId().equals(parentUserId)) {
            throw new IllegalStateException("Child account is already linked to another parent");
        }

        // Update child settings
        childSettings.setIsChildAccount(true);
        childSettings.setParentUserId(parentUserId);
        
        if (request.getAgeGroup() != null) {
            childSettings.setAgeGroup(request.getAgeGroup());
        }
        if (request.getContentRestrictionLevel() != null) {
            childSettings.setContentRestrictionLevel(request.getContentRestrictionLevel());
        } else {
            childSettings.setContentRestrictionLevel("MODERATE"); // Default for new child accounts
        }
        if (request.getRequireParentApproval() != null) {
            childSettings.setRequireParentApproval(request.getRequireParentApproval());
        } else {
            childSettings.setRequireParentApproval(true); // Default for new child accounts
        }

        parentalRepository.save(childSettings);
        log.info("Successfully linked child {} to parent {}", request.getChildUserId(), parentUserId);

        return toChildDTO(child, childSettings);
    }

    /**
     * Unlink a child account from parent
     */
    @Transactional
    public void unlinkChildAccount(Long parentUserId, Long childUserId) {
        log.info("Parent {} unlinking child account {}", parentUserId, childUserId);

        UserParentalSettings childSettings = parentalRepository.findByUserId(childUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Child parental settings not found"));

        // Verify parent ownership
        if (!parentUserId.equals(childSettings.getParentUserId())) {
            throw new IllegalStateException("You are not the parent of this child account");
        }

        // Reset child settings
        childSettings.setIsChildAccount(false);
        childSettings.setParentUserId(null);
        childSettings.setContentRestrictionLevel("NONE");
        childSettings.setRequireParentApproval(false);

        parentalRepository.save(childSettings);
        log.info("Successfully unlinked child {} from parent {}", childUserId, parentUserId);
    }

    /**
     * Get all linked children for a parent
     */
    @Transactional(readOnly = true)
    public List<ChildAccountDTO> getLinkedChildren(Long parentUserId) {
        log.debug("Getting linked children for parent {}", parentUserId);
        
        List<UserParentalSettings> childSettings = parentalRepository.findChildrenByParentId(parentUserId);
        
        return childSettings.stream()
            .map(settings -> {
                User child = userRepository.findById(settings.getUser().getId())
                    .orElse(null);
                return child != null ? toChildDTO(child, settings) : null;
            })
            .filter(dto -> dto != null)
            .collect(Collectors.toList());
    }

    /**
     * Update a specific child's settings (by parent)
     */
    @Transactional
    public ChildAccountDTO updateChildSettings(Long parentUserId, Long childUserId, UpdateParentalSettingsRequest request) {
        log.info("Parent {} updating settings for child {}", parentUserId, childUserId);

        UserParentalSettings childSettings = parentalRepository.findByUserId(childUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Child parental settings not found"));

        // Verify parent ownership
        if (!parentUserId.equals(childSettings.getParentUserId())) {
            throw new IllegalStateException("You are not the parent of this child account");
        }

        // Update allowed fields
        if (request.getAgeGroup() != null) {
            childSettings.setAgeGroup(request.getAgeGroup());
        }
        if (request.getContentRestrictionLevel() != null) {
            childSettings.setContentRestrictionLevel(request.getContentRestrictionLevel());
        }
        if (request.getRequireParentApproval() != null) {
            childSettings.setRequireParentApproval(request.getRequireParentApproval());
        }
        if (request.getAllowedTopicCategories() != null) {
            childSettings.setAllowedTopicCategories(toJson(request.getAllowedTopicCategories()));
        }
        if (request.getBlockedTopicCategories() != null) {
            childSettings.setBlockedTopicCategories(toJson(request.getBlockedTopicCategories()));
        }
        if (request.getMaxDailyScreenTimeMinutes() != null) {
            childSettings.setMaxDailyScreenTimeMinutes(request.getMaxDailyScreenTimeMinutes());
        }
        if (request.getMaxDailyQuizCount() != null) {
            childSettings.setMaxDailyQuizCount(request.getMaxDailyQuizCount());
        }

        parentalRepository.save(childSettings);

        User child = userRepository.findById(childUserId)
            .orElseThrow(() -> new ResourceNotFoundException("Child user not found"));

        return toChildDTO(child, childSettings);
    }

    /**
     * Verify parent PIN
     */
    @Transactional
    public boolean verifyParentPin(Long userId, String pin) {
        UserParentalSettings settings = parentalRepository.findByUserId(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Parental settings not found"));

        if (!StringUtils.hasText(settings.getParentPinHash())) {
            throw new IllegalStateException("No parent PIN has been set");
        }

        boolean valid = passwordEncoder.matches(pin, settings.getParentPinHash());
        
        if (valid) {
            settings.setLastParentVerification(LocalDateTime.now());
            parentalRepository.save(settings);
        }

        return valid;
    }

    /**
     * Check if user is a child account
     */
    @Transactional(readOnly = true)
    public boolean isChildAccount(Long userId) {
        return parentalRepository.existsByUserIdAndIsChildAccountTrue(userId);
    }

    /**
     * Check if content is allowed for user based on parental settings
     */
    @Transactional(readOnly = true)
    public boolean isContentAllowed(Long userId, String topicCategory) {
        UserParentalSettings settings = parentalRepository.findByUserId(userId).orElse(null);
        
        if (settings == null || !Boolean.TRUE.equals(settings.getIsChildAccount())) {
            return true; // No restrictions for non-child accounts
        }

        // Check blocked categories
        List<String> blocked = fromJson(settings.getBlockedTopicCategories());
        if (blocked != null && blocked.contains(topicCategory)) {
            return false;
        }

        // Check allowed categories (if specified, only those are allowed)
        List<String> allowed = fromJson(settings.getAllowedTopicCategories());
        if (allowed != null && !allowed.isEmpty()) {
            return allowed.contains(topicCategory);
        }

        return true;
    }

    // ========== PRIVATE HELPER METHODS ========== 

    private UserParentalSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        log.info("Creating default parental settings for user {}", userId);
        
        UserParentalSettings settings = UserParentalSettings.builder()
            .user(user)
            .isChildAccount(false)
            .ageGroup("ADULT")
            .contentRestrictionLevel("NONE")
            .requireParentApproval(false)
            .build();

        return parentalRepository.save(settings);
    }

    private UserParentalSettingsDTO toDTO(UserParentalSettings settings) {
        String parentUsername = null;
        if (settings.getParentUserId() != null) {
            parentUsername = userRepository.findById(settings.getParentUserId())
                .map(User::getUsername)
                .orElse(null);
        }

        long childrenCount = parentalRepository.countChildrenByParentId(settings.getUser().getId());

        return UserParentalSettingsDTO.builder()
            .id(settings.getId())
            .userId(settings.getUser().getId())
            .isChildAccount(settings.getIsChildAccount())
            .parentUserId(settings.getParentUserId())
            .parentUsername(parentUsername)
            .ageGroup(settings.getAgeGroup())
            .contentRestrictionLevel(settings.getContentRestrictionLevel())
            .requireParentApproval(settings.getRequireParentApproval())
            .allowedTopicCategories(fromJson(settings.getAllowedTopicCategories()))
            .blockedTopicCategories(fromJson(settings.getBlockedTopicCategories()))
            .maxDailyScreenTimeMinutes(settings.getMaxDailyScreenTimeMinutes())
            .maxDailyQuizCount(settings.getMaxDailyQuizCount())
            .hasParentPin(StringUtils.hasText(settings.getParentPinHash()))
            .lastParentVerification(settings.getLastParentVerification())
            .createdAt(settings.getCreatedAt())
            .updatedAt(settings.getUpdatedAt())
            .linkedChildrenCount((int) childrenCount)
            .build();
    }

    private ChildAccountDTO toChildDTO(User child, UserParentalSettings settings) {
        return ChildAccountDTO.builder()
            .userId(child.getId())
            .username(child.getUsername())
            .email(child.getEmail())
            .ageGroup(settings.getAgeGroup())
            .contentRestrictionLevel(settings.getContentRestrictionLevel())
            .requireParentApproval(settings.getRequireParentApproval())
            .maxDailyScreenTimeMinutes(settings.getMaxDailyScreenTimeMinutes())
            .maxDailyQuizCount(settings.getMaxDailyQuizCount())
            .linkedAt(settings.getUpdatedAt())
            .build();
    }

    private void validatePin(String pin) {
        if (!pin.matches("^\\d{4,6}$")) {
            throw new IllegalArgumentException("PIN must be 4-6 digits");
        }
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize list to JSON", e);
            return String.join(",", list);
        }
    }

    private List<String> fromJson(String json) {
        if (!StringUtils.hasText(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            // Fallback to comma-separated
            return List.of(json.split(","));
        }
    }
}
