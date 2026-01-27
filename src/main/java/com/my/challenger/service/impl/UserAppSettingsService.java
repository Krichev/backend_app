package com.my.challenger.service.impl;

import com.my.challenger.dto.settings.UpdateAppSettingsRequest;
import com.my.challenger.dto.settings.UserAppSettingsDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserAppSettings;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserAppSettingsRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserAppSettingsService {

    private final UserAppSettingsRepository settingsRepository;
    private final UserRepository userRepository;

    /**
     * Get user app settings, creating default if not exists
     */
    @Transactional
    public UserAppSettingsDTO getOrCreateSettings(Long userId) {
        log.debug("Getting app settings for user {}", userId);
        UserAppSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));
        return toDTO(settings);
    }

    /**
     * Update user app settings (partial update)
     */
    @Transactional
    public UserAppSettingsDTO updateSettings(Long userId, UpdateAppSettingsRequest request) {
        log.info("Updating app settings for user {}", userId);
        
        UserAppSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));

        // Partial update - only update fields that are provided
        if (request.getLanguage() != null) {
            settings.setLanguage(request.getLanguage());
            log.info("Updated language to '{}' for user {}", request.getLanguage(), userId);
        }
        if (request.getTheme() != null) {
            settings.setTheme(request.getTheme());
            log.info("Updated theme to '{}' for user {}", request.getTheme(), userId);
        }
        if (request.getNotificationsEnabled() != null) {
            settings.setNotificationsEnabled(request.getNotificationsEnabled());
            log.info("Updated notifications to '{}' for user {}", request.getNotificationsEnabled(), userId);
        }

        settings = settingsRepository.save(settings);
        return toDTO(settings);
    }

    /**
     * Quick update for language only
     */
    @Transactional
    public UserAppSettingsDTO updateLanguage(Long userId, String language) {
        if (!language.matches("^(en|ru)$")) {
            throw new IllegalArgumentException("Language must be 'en' or 'ru'");
        }
        
        log.info("Updating language to '{}' for user {}", language, userId);
        
        UserAppSettings settings = settingsRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));
        
        settings.setLanguage(language);
        settings = settingsRepository.save(settings);
        
        return toDTO(settings);
    }

    /**
     * Create default settings for a user
     */
    private UserAppSettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        log.info("Creating default app settings for user {}", userId);
        
        UserAppSettings settings = UserAppSettings.builder()
            .user(user)
            .language("en")
            .theme("system")
            .notificationsEnabled(true)
            .build();

        return settingsRepository.save(settings);
    }

    /**
     * Convert entity to DTO
     */
    private UserAppSettingsDTO toDTO(UserAppSettings settings) {
        return UserAppSettingsDTO.builder()
            .id(settings.getId())
            .userId(settings.getUser().getId())
            .language(settings.getLanguage())
            .theme(settings.getTheme())
            .notificationsEnabled(settings.getNotificationsEnabled())
            .createdAt(settings.getCreatedAt())
            .updatedAt(settings.getUpdatedAt())
            .build();
    }
}
