package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.UpdatePrivacySettingsRequest;
import com.my.challenger.dto.quiz.UserPrivacySettingsDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserPrivacySettings;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserPrivacySettingsRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPrivacySettingsService {

    private final UserPrivacySettingsRepository privacyRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserPrivacySettingsDTO getPrivacySettings(Long userId) {
        UserPrivacySettings settings = privacyRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));
        return toDTO(settings);
    }

    @Transactional
    public UserPrivacySettingsDTO updatePrivacySettings(Long userId, UpdatePrivacySettingsRequest request) {
        UserPrivacySettings settings = privacyRepository.findByUserId(userId)
            .orElseGet(() -> createDefaultSettings(userId));

        if (request.getAllowRequestsFrom() != null) {
            settings.setAllowRequestsFrom(request.getAllowRequestsFrom());
        }
        if (request.getShowConnections() != null) {
            settings.setShowConnections(request.getShowConnections());
        }
        if (request.getShowMutualConnections() != null) {
            settings.setShowMutualConnections(request.getShowMutualConnections());
        }

        settings = privacyRepository.save(settings);
        return toDTO(settings);
    }

    private UserPrivacySettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        UserPrivacySettings settings = UserPrivacySettings.builder()
            .user(user)
            .allowRequestsFrom("ANYONE")
            .showConnections(true)
            .showMutualConnections(true)
            .build();
        
        return privacyRepository.save(settings);
    }

    private UserPrivacySettingsDTO toDTO(UserPrivacySettings settings) {
        return UserPrivacySettingsDTO.builder()
            .allowRequestsFrom(settings.getAllowRequestsFrom())
            .showConnections(settings.getShowConnections())
            .showMutualConnections(settings.getShowMutualConnections())
            .build();
    }
}
