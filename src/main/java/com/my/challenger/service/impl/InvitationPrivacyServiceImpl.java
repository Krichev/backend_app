package com.my.challenger.service.impl;

import com.my.challenger.dto.invitation.UpdateInvitationPreferencesRequest;
import com.my.challenger.dto.invitation.UserInvitationPreferencesDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserPrivacySettings;
import com.my.challenger.entity.enums.Gender;
import com.my.challenger.entity.enums.GenderPreference;
import com.my.challenger.entity.enums.InvitationPreference;
import com.my.challenger.entity.enums.RelationshipType;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserPrivacySettingsRepository;
import com.my.challenger.repository.UserRelationshipRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.InvitationPrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvitationPrivacyServiceImpl implements InvitationPrivacyService {

    private final UserPrivacySettingsRepository privacySettingsRepository;
    private final UserRelationshipRepository userRelationshipRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean canUserInvite(Long inviterId, Long inviteeId) {
        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", inviterId));
        
        UserPrivacySettings settings = privacySettingsRepository.findByUserId(inviteeId)
                .orElseGet(() -> createDefaultSettings(inviteeId));

        if (!checkInvitationPreference(inviter, inviteeId, settings.getQuestInvitationPreference())) {
            return false;
        }

        return checkGenderPreference(inviter, settings.getGenderPreferenceForInvites());
    }

    @Override
    @Transactional(readOnly = true)
    public UserInvitationPreferencesDTO getPreferences(Long userId) {
        UserPrivacySettings settings = privacySettingsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("PrivacySettings", "userId", userId));
        
        return UserInvitationPreferencesDTO.builder()
                .userId(userId)
                .questInvitationPreference(settings.getQuestInvitationPreference())
                .genderPreferenceForInvites(settings.getGenderPreferenceForInvites())
                .build();
    }

    @Override
    @Transactional
    public UserInvitationPreferencesDTO updatePreferences(Long userId, UpdateInvitationPreferencesRequest request) {
        UserPrivacySettings settings = privacySettingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(userId));
        
        settings.setQuestInvitationPreference(request.getQuestInvitationPreference());
        settings.setGenderPreferenceForInvites(request.getGenderPreferenceForInvites());
        
        UserPrivacySettings saved = privacySettingsRepository.save(settings);
        
        return UserInvitationPreferencesDTO.builder()
                .userId(userId)
                .questInvitationPreference(saved.getQuestInvitationPreference())
                .genderPreferenceForInvites(saved.getGenderPreferenceForInvites())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> filterInvitableUsers(Long inviterId, List<Long> candidateUserIds) {
        return candidateUserIds.stream()
                .filter(inviteeId -> canUserInvite(inviterId, inviteeId))
                .collect(Collectors.toList());
    }

    private UserPrivacySettings createDefaultSettings(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
        
        UserPrivacySettings settings = UserPrivacySettings.builder()
                .user(user)
                .questInvitationPreference(InvitationPreference.ANYONE)
                .genderPreferenceForInvites(GenderPreference.ANY_GENDER)
                .build();
                
        return privacySettingsRepository.save(settings);
    }

    private boolean checkInvitationPreference(User inviter, Long inviteeId, InvitationPreference preference) {
        switch (preference) {
            case NOBODY:
                return false;
            case ANYONE:
                return true;
            case FRIENDS_ONLY:
                return isFriend(inviter.getId(), inviteeId);
            case FAMILY_ONLY:
                return isFamily(inviter.getId(), inviteeId);
            case FRIENDS_AND_FAMILY:
                return userRelationshipRepository.areUsersConnected(inviter.getId(), inviteeId);
            default:
                return true;
        }
    }

    private boolean isFriend(Long inviterId, Long inviteeId) {
        return userRelationshipRepository.findBetweenUsers(inviterId, inviteeId)
                .map(rel -> RelationshipType.FRIEND.equals(rel.getRelationshipType()))
                .orElse(false);
    }
    
    private boolean isFamily(Long inviterId, Long inviteeId) {
        return userRelationshipRepository.findBetweenUsers(inviterId, inviteeId)
                .map(rel -> {
                    RelationshipType type = rel.getRelationshipType();
                    return type == RelationshipType.FAMILY_PARENT || 
                           type == RelationshipType.FAMILY_SIBLING || 
                           type == RelationshipType.FAMILY_EXTENDED;
                })
                .orElse(false);
    }

    private boolean checkGenderPreference(User inviter, GenderPreference preference) {
        if (preference == GenderPreference.ANY_GENDER) {
            return true;
        }
        if (inviter.getGender() == null) {
            return false;
        }
        if (preference == GenderPreference.MALE_ONLY) {
            return inviter.getGender() == Gender.MALE;
        }
        if (preference == GenderPreference.FEMALE_ONLY) {
            return inviter.getGender() == Gender.FEMALE;
        }
        return true;
    }
}