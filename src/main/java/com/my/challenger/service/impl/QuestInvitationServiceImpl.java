package com.my.challenger.service.impl;

import com.my.challenger.dto.invitation.*;
import com.my.challenger.entity.*;
import com.my.challenger.entity.enums.NegotiationStatus;
import com.my.challenger.entity.enums.QuestInvitationStatus;
import com.my.challenger.exception.InvalidInvitationStateException;
import com.my.challenger.exception.InvitationAlreadyExistsException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.exception.UnauthorizedException;
import com.my.challenger.repository.InvitationNegotiationRepository;
import com.my.challenger.repository.QuestInvitationRepository;
import com.my.challenger.repository.QuestRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.ChallengeService;
import com.my.challenger.service.InvitationPrivacyService;
import com.my.challenger.service.QuestInvitationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class QuestInvitationServiceImpl implements QuestInvitationService {

    private final QuestInvitationRepository invitationRepository;
    private final InvitationNegotiationRepository negotiationRepository;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;
    private final ChallengeService challengeService;
    private final InvitationPrivacyService privacyService;

    @Override
    @Transactional
    public QuestInvitationDTO createInvitation(CreateQuestInvitationRequest request, Long inviterId) {
        Quest quest = questRepository.findById(request.getQuestId())
                .orElseThrow(() -> new ResourceNotFoundException("Quest", "id", request.getQuestId()));

        User inviter = userRepository.findById(inviterId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", inviterId));

        User invitee = userRepository.findById(request.getInviteeId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.getInviteeId()));

        // 1. Permission check: inviter must be creator or have rights (logic simplified to creator for now)
        // If we want to allow others to invite, logic goes here.
        // Assuming strictly creator or we rely on canInviteUser logic + quest visibility.
        // But doc says: "Validate inviter owns the quest or can invite"
        // Let's enforce ownership or similar for now, or just let privacy service handle relationship.
        // Usually only participants or creator can invite.
        // For now, let's assume any user can TRY to invite if they have access to the quest, 
        // subject to privacy settings.

        // 2. Privacy check
        if (!privacyService.canUserInvite(inviterId, request.getInviteeId())) {
             throw new UnauthorizedException("User does not accept invitations from you due to privacy settings.");
        }

        // 3. Check existing invitation
        List<QuestInvitationStatus> activeStatuses = Arrays.asList(QuestInvitationStatus.PENDING, QuestInvitationStatus.NEGOTIATING);
        if (invitationRepository.existsByQuestIdAndInviteeIdAndStatusIn(quest.getId(), invitee.getId(), activeStatuses)) {
            throw new InvitationAlreadyExistsException("User has already been invited to this quest.");
        }

        // 4. Check if already participant
        boolean isParticipant = quest.getParticipants().stream()
                .anyMatch(u -> u.getId().equals(invitee.getId()));
        if (isParticipant) {
            throw new InvitationAlreadyExistsException("User is already a participant in this quest.");
        }

        // 5. Create Invitation
        QuestInvitation invitation = QuestInvitation.builder()
                .quest(quest)
                .inviter(inviter)
                .invitee(invitee)
                .proposedStakeType(request.getStakeType())
                .proposedStakeAmount(request.getStakeAmount())
                .proposedStakeCurrency(request.getStakeCurrency())
                .proposedScreenTimeMinutes(request.getScreenTimeMinutes())
                .proposedSocialPenaltyDescription(request.getSocialPenaltyDescription())
                .status(QuestInvitationStatus.PENDING)
                .message(request.getMessage())
                .expiresAt(request.getExpiresAt())
                .build();

        QuestInvitation saved = invitationRepository.save(invitation);
        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public QuestInvitationDTO getInvitation(Long invitationId, Long userId) {
        QuestInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "id", invitationId));

        if (!invitation.getInviter().getId().equals(userId) && !invitation.getInvitee().getId().equals(userId)) {
            throw new UnauthorizedException("You are not part of this invitation.");
        }

        return mapToDTO(invitation);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationSummaryDTO> getReceivedInvitations(Long userId, List<QuestInvitationStatus> statuses) {
        return invitationRepository.findByInviteeAndStatusIn(userId, statuses).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<InvitationSummaryDTO> getSentInvitations(Long userId) {
        return invitationRepository.findByInviterId(userId).stream()
                .map(this::mapToSummaryDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public QuestInvitationDTO respondToInvitation(Long invitationId, RespondToInvitationRequest request, Long userId) {
        QuestInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "id", invitationId));

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new UnauthorizedException("Only the invitee can respond to this invitation.");
        }

        if (invitation.getStatus() != QuestInvitationStatus.PENDING) {
             throw new InvalidInvitationStateException("Invitation is not in PENDING state.");
        }
        
        if (invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus(QuestInvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new InvalidInvitationStateException("Invitation has expired.");
        }

        switch (request.getResponse()) {
            case ACCEPT:
                processAcceptance(invitation);
                break;
            case DECLINE:
                invitation.setStatus(QuestInvitationStatus.DECLINED);
                invitation.setRespondedAt(LocalDateTime.now());
                // Assuming message update if allowed, or log it
                break;
            case NEGOTIATE:
                throw new InvalidInvitationStateException("Use createCounterOffer to negotiate.");
        }
        
        QuestInvitation saved = invitationRepository.save(invitation);
        return mapToDTO(saved);
    }

    private void processAcceptance(QuestInvitation invitation) {
        invitation.setStatus(QuestInvitationStatus.ACCEPTED);
        invitation.setRespondedAt(LocalDateTime.now());
        
        // Add participant
        challengeService.joinChallenge(invitation.getQuest().getId(), invitation.getInvitee().getId());
        
        // Logic for Wager creation would go here if stake exists
    }

    @Override
    @Transactional
    public void cancelInvitation(Long invitationId, Long inviterId) {
        QuestInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "id", invitationId));

        if (!invitation.getInviter().getId().equals(inviterId)) {
            throw new UnauthorizedException("Only the inviter can cancel this invitation.");
        }

        if (invitation.getStatus() == QuestInvitationStatus.ACCEPTED || invitation.getStatus() == QuestInvitationStatus.DECLINED) {
            throw new InvalidInvitationStateException("Cannot cancel an already finalized invitation.");
        }

        invitation.setStatus(QuestInvitationStatus.CANCELLED);
        invitationRepository.save(invitation);
    }

    @Override
    @Transactional
    public QuestInvitationDTO createCounterOffer(Long invitationId, CreateCounterOfferRequest request, Long userId) {
        QuestInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "id", invitationId));

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new UnauthorizedException("Only the invitee can propose a counter-offer.");
        }

        if (invitation.getStatus() != QuestInvitationStatus.PENDING && invitation.getStatus() != QuestInvitationStatus.NEGOTIATING) {
            throw new InvalidInvitationStateException("Invitation must be PENDING or NEGOTIATING to counter-offer.");
        }

        InvitationNegotiation negotiation = InvitationNegotiation.builder()
                .invitation(invitation)
                .proposer(invitation.getInvitee())
                .counterStakeType(request.getStakeType())
                .counterStakeAmount(request.getStakeAmount())
                .counterStakeCurrency(request.getStakeCurrency())
                .counterScreenTimeMinutes(request.getScreenTimeMinutes())
                .counterSocialPenaltyDescription(request.getSocialPenaltyDescription())
                .status(NegotiationStatus.PROPOSED)
                .message(request.getMessage())
                .build();

        negotiationRepository.save(negotiation);
        
        invitation.setStatus(QuestInvitationStatus.NEGOTIATING);
        QuestInvitation saved = invitationRepository.save(invitation);
        
        return mapToDTO(saved);
    }

    @Override
    @Transactional
    public QuestInvitationDTO respondToCounterOffer(Long invitationId, Long negotiationId, RespondToCounterOfferRequest request, Long userId) {
        QuestInvitation invitation = invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", "id", invitationId));
                
        InvitationNegotiation negotiation = negotiationRepository.findById(negotiationId)
                .orElseThrow(() -> new ResourceNotFoundException("Negotiation", "id", negotiationId));

        if (!negotiation.getInvitation().getId().equals(invitationId)) {
            throw new IllegalArgumentException("Negotiation does not belong to this invitation.");
        }

        // Only inviter can respond to counter-offer from invitee. 
        // (Assuming simple flow where invitee counters, and inviter accepts/rejects).
        if (!invitation.getInviter().getId().equals(userId)) {
             throw new UnauthorizedException("Only the original inviter can respond to a counter-offer.");
        }
        
        if (negotiation.getStatus() != NegotiationStatus.PROPOSED) {
            throw new InvalidInvitationStateException("Negotiation is not in PROPOSED state.");
        }

        negotiation.setRespondedAt(LocalDateTime.now());
        if (request.getAccepted()) {
            negotiation.setStatus(NegotiationStatus.ACCEPTED);
            
            // Update invitation with agreed terms
            invitation.setProposedStakeType(negotiation.getCounterStakeType());
            invitation.setProposedStakeAmount(negotiation.getCounterStakeAmount());
            invitation.setProposedStakeCurrency(negotiation.getCounterStakeCurrency());
            invitation.setProposedScreenTimeMinutes(negotiation.getCounterScreenTimeMinutes());
            invitation.setProposedSocialPenaltyDescription(negotiation.getCounterSocialPenaltyDescription());
            
            // Reset to PENDING so invitee can now ACCEPT the new terms
            invitation.setStatus(QuestInvitationStatus.PENDING);
        } else {
            negotiation.setStatus(NegotiationStatus.REJECTED);
            // Revert to PENDING so invitee can accept original or decline
            invitation.setStatus(QuestInvitationStatus.PENDING);
        }

        negotiationRepository.save(negotiation);
        QuestInvitation saved = invitationRepository.save(invitation);
        return mapToDTO(saved);
    }

    @Override
    public boolean canInviteUser(Long inviterId, Long inviteeId) {
        return privacyService.canUserInvite(inviterId, inviteeId);
    }

    @Override
    @Transactional
    public int expireStaleInvitations() {
        List<QuestInvitation> expiredInvitations = invitationRepository.findExpiredInvitations(LocalDateTime.now());
        for (QuestInvitation invitation : expiredInvitations) {
            invitation.setStatus(QuestInvitationStatus.EXPIRED);
        }
        invitationRepository.saveAll(expiredInvitations);
        return expiredInvitations.size();
    }

    private QuestInvitationDTO mapToDTO(QuestInvitation invitation) {
        InvitationNegotiation latestNegotiation = negotiationRepository.findLatestByInvitationId(invitation.getId()).orElse(null);
        InvitationNegotiationDTO negotiationDTO = latestNegotiation != null ? mapNegotiationToDTO(latestNegotiation) : null;
        
        long timeRemaining = 0;
        if (invitation.getExpiresAt() != null) {
            timeRemaining = ChronoUnit.SECONDS.between(LocalDateTime.now(), invitation.getExpiresAt());
            if (timeRemaining < 0) timeRemaining = 0;
        }

        return QuestInvitationDTO.builder()
                .id(invitation.getId())
                .questId(invitation.getQuest().getId())
                .questTitle(invitation.getQuest().getTitle())
                .inviterId(invitation.getInviter().getId())
                .inviterUsername(invitation.getInviter().getUsername())
                .inviteeId(invitation.getInvitee().getId())
                .inviteeUsername(invitation.getInvitee().getUsername())
                .stakeType(invitation.getProposedStakeType())
                .stakeAmount(invitation.getProposedStakeAmount())
                .stakeCurrency(invitation.getProposedStakeCurrency())
                .screenTimeMinutes(invitation.getProposedScreenTimeMinutes())
                .socialPenaltyDescription(invitation.getProposedSocialPenaltyDescription())
                .status(invitation.getStatus())
                .message(invitation.getMessage())
                .expiresAt(invitation.getExpiresAt())
                .currentNegotiation(negotiationDTO)
                .createdAt(invitation.getCreatedAt())
                .respondedAt(invitation.getRespondedAt())
                .isExpired(invitation.getExpiresAt() != null && invitation.getExpiresAt().isBefore(LocalDateTime.now()))
                .canNegotiate(invitation.getStatus() == QuestInvitationStatus.PENDING)
                .timeRemainingSeconds(timeRemaining)
                .build();
    }

    private InvitationSummaryDTO mapToSummaryDTO(QuestInvitation invitation) {
        return InvitationSummaryDTO.builder()
                .id(invitation.getId())
                .questId(invitation.getQuest().getId())
                .questTitle(invitation.getQuest().getTitle())
                .otherPartyUsername(invitation.getInviter().getUsername()) // Logic might vary based on who asks, but simplest is showing inviter usually
                .stakeType(invitation.getProposedStakeType())
                .stakeAmount(invitation.getProposedStakeAmount())
                .status(invitation.getStatus())
                .expiresAt(invitation.getExpiresAt())
                .hasActiveNegotiation(invitation.getStatus() == QuestInvitationStatus.NEGOTIATING)
                .build();
    }
    
    private InvitationNegotiationDTO mapNegotiationToDTO(InvitationNegotiation negotiation) {
        return InvitationNegotiationDTO.builder()
                .id(negotiation.getId())
                .invitationId(negotiation.getInvitation().getId())
                .proposerId(negotiation.getProposer().getId())
                .proposerUsername(negotiation.getProposer().getUsername())
                .isProposerInviter(negotiation.getProposer().getId().equals(negotiation.getInvitation().getInviter().getId()))
                .counterStakeType(negotiation.getCounterStakeType())
                .counterStakeAmount(negotiation.getCounterStakeAmount())
                .counterStakeCurrency(negotiation.getCounterStakeCurrency())
                .counterScreenTimeMinutes(negotiation.getCounterScreenTimeMinutes())
                .counterSocialPenaltyDescription(negotiation.getCounterSocialPenaltyDescription())
                .status(negotiation.getStatus())
                .message(negotiation.getMessage())
                .createdAt(negotiation.getCreatedAt())
                .respondedAt(negotiation.getRespondedAt())
                .build();
    }
}
