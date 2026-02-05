package com.my.challenger.service;

import com.my.challenger.dto.invitation.UpdateInvitationPreferencesRequest;
import com.my.challenger.dto.invitation.UserInvitationPreferencesDTO;
import java.util.List;

public interface InvitationPrivacyService {
    boolean canUserInvite(Long inviterId, Long inviteeId);
    UserInvitationPreferencesDTO getPreferences(Long userId);
    UserInvitationPreferencesDTO updatePreferences(Long userId, UpdateInvitationPreferencesRequest request);
    List<Long> filterInvitableUsers(Long inviterId, List<Long> candidateUserIds);
}
