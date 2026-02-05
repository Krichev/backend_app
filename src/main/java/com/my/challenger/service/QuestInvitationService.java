package com.my.challenger.service;

import com.my.challenger.dto.invitation.*;
import com.my.challenger.entity.enums.QuestInvitationStatus;
import java.util.List;

public interface QuestInvitationService {
    // Invitation CRUD
    QuestInvitationDTO createInvitation(CreateQuestInvitationRequest request, Long inviterId);
    QuestInvitationDTO getInvitation(Long invitationId, Long userId);
    List<InvitationSummaryDTO> getReceivedInvitations(Long userId, List<QuestInvitationStatus> statuses);
    List<InvitationSummaryDTO> getSentInvitations(Long userId);
    
    // Invitation responses
    QuestInvitationDTO respondToInvitation(Long invitationId, RespondToInvitationRequest request, Long userId);
    void cancelInvitation(Long invitationId, Long inviterId);
    
    // Counter-offer handling
    QuestInvitationDTO createCounterOffer(Long invitationId, CreateCounterOfferRequest request, Long userId);
    QuestInvitationDTO respondToCounterOffer(Long invitationId, Long negotiationId, RespondToCounterOfferRequest request, Long userId);
    
    // Utility
    boolean canInviteUser(Long inviterId, Long inviteeId);
    int expireStaleInvitations();
}
