package com.my.challenger.service.impl;

import com.my.challenger.dto.wager.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.challenge.Challenge;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizSession;
import com.my.challenger.entity.wager.Wager;
import com.my.challenger.entity.wager.WagerOutcome;
import com.my.challenger.entity.wager.WagerParticipant;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.*;
import com.my.challenger.service.PenaltyService;
import com.my.challenger.service.WagerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WagerServiceImpl implements WagerService {

    private final WagerRepository wagerRepository;
    private final WagerParticipantRepository participantRepository;
    private final WagerOutcomeRepository outcomeRepository;
    private final UserRepository userRepository;
    private final ChallengeRepository challengeRepository;
    private final QuizSessionRepository quizSessionRepository;
    private final EnhancedPaymentService paymentService;
    private final PenaltyService penaltyService;

    @Override
    @Transactional
    public WagerDTO createWager(CreateWagerRequest request, Long creatorUserId) {
        log.info("Creating wager for user {} on challenge {}", creatorUserId, request.getChallengeId());

        User creator = userRepository.findById(creatorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", creatorUserId));

        Challenge challenge = challengeRepository.findById(request.getChallengeId())
                .orElseThrow(() -> new ResourceNotFoundException("Challenge", "id", request.getChallengeId()));

        if (challenge.getStatus() != ChallengeStatus.ACTIVE && challenge.getStatus() != ChallengeStatus.OPEN) {
            throw new IllegalStateException("Cannot create wager for a non-active challenge");
        }

        QuizSession quizSession = null;
        if (request.getQuizSessionId() != null) {
            quizSession = quizSessionRepository.findById(request.getQuizSessionId())
                    .orElseThrow(() -> new ResourceNotFoundException("QuizSession", "id", request.getQuizSessionId()));
        }

        // Validate stake
        validateStake(request, creator);

        Wager wager = Wager.builder()
                .challenge(challenge)
                .quizSession(quizSession)
                .creator(creator)
                .wagerType(request.getWagerType())
                .stakeType(request.getStakeType())
                .stakeAmount(request.getStakeAmount())
                .stakeCurrency(request.getStakeCurrency())
                .status(WagerStatus.PROPOSED)
                .minParticipants(request.getMinParticipants() != null ? request.getMinParticipants() : 2)
                .maxParticipants(request.getMaxParticipants())
                .socialPenaltyDescription(request.getSocialPenaltyDescription())
                .screenTimeMinutes(request.getScreenTimeMinutes())
                .expiresAt(request.getExpiresAt() != null ? request.getExpiresAt() : LocalDateTime.now().plusHours(24))
                .participants(new ArrayList<>())
                .build();

        wager = wagerRepository.save(wager);

        // Add creator as first participant
        WagerParticipant creatorParticipant = WagerParticipant.builder()
                .wager(wager)
                .user(creator)
                .status(ParticipantWagerStatus.ACCEPTED)
                .build();

        // Escrow creator's stake
        escrowStake(creatorParticipant, wager);
        
        participantRepository.save(creatorParticipant);
        wager.getParticipants().add(creatorParticipant);

        // Add invited users
        if (request.getInvitedUserIds() != null) {
            for (Long invitedId : request.getInvitedUserIds()) {
                if (invitedId.equals(creatorUserId)) continue;
                
                User invitedUser = userRepository.findById(invitedId)
                        .orElseThrow(() -> new ResourceNotFoundException("User", "id", invitedId));
                
                WagerParticipant invitedParticipant = WagerParticipant.builder()
                        .wager(wager)
                        .user(invitedUser)
                        .status(ParticipantWagerStatus.INVITED)
                        .build();
                
                participantRepository.save(invitedParticipant);
                wager.getParticipants().add(invitedParticipant);
            }
        }

        return mapToDTO(wager);
    }

    @Override
    @Transactional
    public WagerDTO acceptWager(Long wagerId, Long userId) {
        log.info("User {} accepting wager {}", userId, wagerId);

        Wager wager = wagerRepository.findById(wagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wager", "id", wagerId));

        if (wager.getStatus() != WagerStatus.PROPOSED) {
            throw new IllegalStateException("Wager is not in PROPOSED state");
        }

        if (wager.getExpiresAt().isBefore(LocalDateTime.now())) {
            wager.setStatus(WagerStatus.EXPIRED);
            wagerRepository.save(wager);
            throw new IllegalStateException("Wager has expired");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        WagerParticipant participant = participantRepository.findByWagerIdAndUserId(wagerId, userId)
                .orElseGet(() -> {
                    if (wager.getMaxParticipants() != null && wager.getParticipants().size() >= wager.getMaxParticipants()) {
                        throw new IllegalStateException("Wager has reached maximum participants");
                    }
                    WagerParticipant newParticipant = WagerParticipant.builder()
                            .wager(wager)
                            .user(user)
                            .status(ParticipantWagerStatus.INVITED)
                            .build();
                    return participantRepository.save(newParticipant);
                });

        if (participant.getStatus() == ParticipantWagerStatus.ACCEPTED) {
            return mapToDTO(wager);
        }

        // Escrow stake
        escrowStake(participant, wager);
        participant.setStatus(ParticipantWagerStatus.ACCEPTED);
        participantRepository.save(participant);

        // Check if we should activate the wager
        long acceptedCount = participantRepository.countByWagerIdAndStatus(wagerId, ParticipantWagerStatus.ACCEPTED);
        if (acceptedCount >= wager.getMinParticipants()) {
            wager.setStatus(WagerStatus.ACTIVE);
            wagerRepository.save(wager);
        }

        return mapToDTO(wager);
    }

    @Override
    @Transactional
    public void declineWager(Long wagerId, Long userId) {
        WagerParticipant participant = participantRepository.findByWagerIdAndUserId(wagerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WagerParticipant", "wagerId:userId", wagerId + ":" + userId));

        if (participant.getStatus() == ParticipantWagerStatus.ACCEPTED) {
            throw new IllegalStateException("Cannot decline an already accepted wager. Use cancel if allowed.");
        }

        participant.setStatus(ParticipantWagerStatus.DECLINED);
        participantRepository.save(participant);

        // If all participants except creator declined, or if it's 1v1 and the other declined
        Wager wager = participant.getWager();
        if (wager.getStatus() == WagerStatus.PROPOSED) {
            long remainingInvitedOrAccepted = participantRepository.findByWagerId(wagerId).stream()
                    .filter(p -> p.getStatus() == ParticipantWagerStatus.INVITED || p.getStatus() == ParticipantWagerStatus.ACCEPTED)
                    .filter(p -> !p.getUser().getId().equals(wager.getCreator().getId()))
                    .count();
            
            if (remainingInvitedOrAccepted == 0) {
                cancelWagerInternal(wager, "All invited participants declined");
            }
        }
    }

    @Override
    @Transactional
    public void cancelWager(Long wagerId, Long userId) {
        Wager wager = wagerRepository.findById(wagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wager", "id", wagerId));

        if (!wager.getCreator().getId().equals(userId)) {
            throw new IllegalStateException("Only the creator can cancel the wager");
        }

        if (wager.getStatus() == WagerStatus.SETTLED || wager.getStatus() == WagerStatus.CANCELLED) {
            throw new IllegalStateException("Wager is already in a terminal state");
        }

        cancelWagerInternal(wager, "Cancelled by creator");
    }

    private void cancelWagerInternal(Wager wager, String reason) {
        log.info("Cancelling wager {}: {}", wager.getId(), reason);
        
        List<WagerParticipant> participants = participantRepository.findByWagerId(wager.getId());
        for (WagerParticipant participant : participants) {
            if (participant.isStakeEscrowed()) {
                refundStake(participant, wager, reason);
            }
        }

        wager.setStatus(WagerStatus.CANCELLED);
        wagerRepository.save(wager);
    }

    @Override
    @Transactional
    public WagerOutcomeDTO settleWager(Long wagerId) {
        Wager wager = wagerRepository.findById(wagerId)
                .orElseThrow(() -> new ResourceNotFoundException("Wager", "id", wagerId));

        if (wager.getStatus() == WagerStatus.SETTLED) {
            WagerOutcome outcome = outcomeRepository.findByWagerId(wagerId).stream().findFirst()
                    .orElseThrow(() -> new IllegalStateException("Wager marked SETTLED but no outcome found"));
            return mapToDTO(outcome);
        }

        if (wager.getStatus() != WagerStatus.ACTIVE) {
            throw new IllegalStateException("Only ACTIVE wagers can be settled");
        }

        List<WagerParticipant> participants = participantRepository.findByWagerId(wagerId).stream()
                .filter(p -> p.getStatus() == ParticipantWagerStatus.ACCEPTED)
                .collect(Collectors.toList());

        if (participants.isEmpty()) {
            wager.setStatus(WagerStatus.CANCELLED);
            wagerRepository.save(wager);
            throw new IllegalStateException("No active participants to settle");
        }

        // Determine winners
        // For simplicity, winner is the one with highest score
        // In case of draws, we might need different logic based on SettlementType
        
        Integer maxScore = participants.stream()
                .map(p -> p.getQuizScore() != null ? p.getQuizScore() : 0)
                .max(Integer::compare)
                .orElse(0);

        List<WagerParticipant> winners = participants.stream()
                .filter(p -> (p.getQuizScore() != null ? p.getQuizScore() : 0) == maxScore.intValue())
                .collect(Collectors.toList());

        List<WagerParticipant> losers = participants.stream()
                .filter(p -> (p.getQuizScore() != null ? p.getQuizScore() : 0) != maxScore.intValue())
                .collect(Collectors.toList());

        BigDecimal totalPot = wager.getStakeAmount().multiply(new BigDecimal(participants.size()));
        
        WagerOutcome outcome;
        if (winners.size() == participants.size() && participants.size() > 1) {
            // It's a draw between everyone
            outcome = handleDraw(wager, participants);
        } else {
            outcome = handleSettlement(wager, winners, losers, totalPot);
        }

        wager.setStatus(WagerStatus.SETTLED);
        wager.setSettledAt(LocalDateTime.now());
        wagerRepository.save(wager);

        return mapToDTO(outcome);
    }

    private WagerOutcome handleDraw(Wager wager, List<WagerParticipant> participants) {
        log.info("Wager {} ended in a draw", wager.getId());
        
        for (WagerParticipant p : participants) {
            p.setStatus(ParticipantWagerStatus.DRAW);
            p.setSettledAt(LocalDateTime.now());
            refundStake(p, wager, "Draw settlement");
            participantRepository.save(p);
        }

        WagerOutcome outcome = WagerOutcome.builder()
                .wager(wager)
                .settlementType(SettlementType.DRAW_REFUND)
                .amountDistributed(BigDecimal.ZERO)
                .notes("Draw between all participants. Stakes refunded.")
                .settledAt(LocalDateTime.now())
                .build();
        
        return outcomeRepository.save(outcome);
    }

    private WagerOutcome handleSettlement(Wager wager, List<WagerParticipant> winners, List<WagerParticipant> losers, BigDecimal totalPot) {
        BigDecimal amountPerWinner = totalPot.divide(new BigDecimal(winners.size()), 2, RoundingMode.HALF_UP);

        for (WagerParticipant winner : winners) {
            winner.setStatus(ParticipantWagerStatus.WON);
            winner.setAmountWon(amountPerWinner);
            winner.setSettledAt(LocalDateTime.now());
            distributeWinnings(winner, amountPerWinner, wager);
            participantRepository.save(winner);
        }

        for (WagerParticipant loser : losers) {
            loser.setStatus(ParticipantWagerStatus.LOST);
            loser.setAmountLost(wager.getStakeAmount());
            loser.setSettledAt(LocalDateTime.now());
            participantRepository.save(loser);
        }

        WagerOutcome outcome = WagerOutcome.builder()
                .wager(wager)
                .winner(winners.size() == 1 ? winners.get(0).getUser() : null)
                .loser(losers.size() == 1 ? losers.get(0).getUser() : null)
                .settlementType(winners.size() > 1 ? SettlementType.PROPORTIONAL : SettlementType.WINNER_TAKES_ALL)
                .amountDistributed(totalPot)
                .notes("Settled successfully. " + winners.size() + " winners.")
                .settledAt(LocalDateTime.now())
                .build();
        
        outcome = outcomeRepository.save(outcome);
        
        // Create penalty for loser if applicable
        if (outcome.getLoser() != null) {
            try {
                penaltyService.createPenaltyFromWager(outcome, wager);
                outcome.setPenaltyAssigned(true);
                outcomeRepository.save(outcome);
            } catch (Exception e) {
                log.error("Failed to create penalty for wager {}", wager.getId(), e);
            }
        }

        return outcome;
    }

    @Override
    @Transactional
    public void updateParticipantScore(Long wagerId, Long userId, Integer score) {
        WagerParticipant participant = participantRepository.findByWagerIdAndUserId(wagerId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("WagerParticipant", "wagerId:userId", wagerId + ":" + userId));
        
        participant.setQuizScore(score);
        participantRepository.save(participant);
    }

    @Override
    @Transactional
    public int expireStaleWagers() {
        LocalDateTime now = LocalDateTime.now();
        List<Wager> expiredWagers = wagerRepository.findExpiredWagers(now);
        
        log.info("Found {} expired wagers to process", expiredWagers.size());
        
        for (Wager wager : expiredWagers) {
            cancelWagerInternal(wager, "Wager expired");
            wager.setStatus(WagerStatus.EXPIRED);
            wagerRepository.save(wager);
        }
        
        return expiredWagers.size();
    }

    @Override
    public List<WagerDTO> getWagersByChallenge(Long challengeId) {
        return wagerRepository.findByChallengeId(challengeId).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<WagerDTO> getActiveWagersByUser(Long userId) {
        return wagerRepository.findByUserIdAndStatus(userId, WagerStatus.ACTIVE).stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<WagerDTO> getWagerHistoryByUser(Long userId, Pageable pageable) {
        return wagerRepository.findByUserId(userId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public WagerDTO getWagerById(Long id) {
        Wager wager = wagerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Wager", "id", id));
        return mapToDTO(wager);
    }

    @Override
    @Transactional
    public void settleWagersForSession(Long sessionId, Long userId, Integer score) {
        log.info("Settling wagers for session {} and user {}", sessionId, userId);
        List<Wager> wagers = wagerRepository.findByQuizSessionId(sessionId);
        for (Wager wager : wagers) {
            if (wager.getStatus() == WagerStatus.ACTIVE) {
                updateParticipantScore(wager.getId(), userId, score);
                
                // If all participants have scores, we can settle
                List<WagerParticipant> participants = participantRepository.findByWagerId(wager.getId()).stream()
                        .filter(p -> p.getStatus() == ParticipantWagerStatus.ACCEPTED)
                        .collect(Collectors.toList());
                
                boolean allScored = participants.stream().allMatch(p -> p.getQuizScore() != null);
                if (allScored) {
                    settleWager(wager.getId());
                }
            }
        }
    }

    // ========== HELPER METHODS ==========

    private void validateStake(CreateWagerRequest request, User creator) {
        if (request.getStakeAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Stake amount must be positive");
        }

        if (request.getStakeType() == StakeType.POINTS) {
            if (!creator.hasEnoughPoints(request.getStakeAmount().longValue())) {
                throw new IllegalArgumentException("Insufficient points for wager");
            }
        } else if (request.getStakeType() == StakeType.MONEY) {
            if (request.getStakeCurrency() == null || request.getStakeCurrency() == CurrencyType.POINTS) {
                throw new IllegalArgumentException("Invalid currency for MONEY stake type");
            }
        } else if (request.getStakeType() == StakeType.SCREEN_TIME) {
            if (request.getScreenTimeMinutes() == null || request.getScreenTimeMinutes() <= 0) {
                throw new IllegalArgumentException("Screen time minutes must be specified for SCREEN_TIME stake type");
            }
        } else if (request.getStakeType() == StakeType.SOCIAL_QUEST) {
            if (request.getSocialPenaltyDescription() == null || request.getSocialPenaltyDescription().isEmpty()) {
                throw new IllegalArgumentException("Social penalty description must be provided for SOCIAL_QUEST stake type");
            }
        }
    }

    private void escrowStake(WagerParticipant participant, Wager wager) {
        if (wager.getStakeType() == StakeType.POINTS) {
            paymentService.deductPoints(participant.getUser(), wager.getStakeAmount().longValue());
            
            // Create a transaction record for escrow
            // Using a separate transaction type or notes to indicate it's a wager deposit
            // Actually EnhancedPaymentService doesn't have a WAGER_ESCROW type, but we can use WITHDRAWAL or DEPOSIT
            // For now, I'll use the existing processEntryFee-like logic but specifically for wagers
        }
        // Other stake types (MONEY, SCREEN_TIME) would have their escrow logic here
        
        participant.setStakeEscrowed(true);
    }

    private void refundStake(WagerParticipant participant, Wager wager, String reason) {
        if (participant.isStakeEscrowed()) {
            if (wager.getStakeType() == StakeType.POINTS) {
                paymentService.addPoints(participant.getUser(), wager.getStakeAmount().longValue());
            }
            participant.setStakeEscrowed(false);
            log.info("Refunded stake to user {} for wager {}: {}", participant.getUser().getId(), wager.getId(), reason);
        }
    }

    private void distributeWinnings(WagerParticipant winner, BigDecimal amount, Wager wager) {
        if (wager.getStakeType() == StakeType.POINTS) {
            paymentService.addPoints(winner.getUser(), amount.longValue());
        }
        // Other stake types (MONEY) would have distribution here
    }

    private WagerDTO mapToDTO(Wager wager) {
        return WagerDTO.builder()
                .id(wager.getId())
                .challengeId(wager.getChallenge().getId())
                .quizSessionId(wager.getQuizSession() != null ? wager.getQuizSession().getId() : null)
                .creatorId(wager.getCreator().getId())
                .creatorUsername(wager.getCreator().getUsername())
                .wagerType(wager.getWagerType())
                .stakeType(wager.getStakeType())
                .stakeAmount(wager.getStakeAmount())
                .stakeCurrency(wager.getStakeCurrency())
                .status(wager.getStatus())
                .minParticipants(wager.getMinParticipants())
                .maxParticipants(wager.getMaxParticipants())
                .socialPenaltyDescription(wager.getSocialPenaltyDescription())
                .screenTimeMinutes(wager.getScreenTimeMinutes())
                .expiresAt(wager.getExpiresAt())
                .settledAt(wager.getSettledAt())
                .createdAt(wager.getCreatedAt())
                .participants(wager.getParticipants().stream().map(this::mapToDTO).collect(Collectors.toList()))
                .build();
    }

    private WagerParticipantDTO mapToDTO(WagerParticipant p) {
        return WagerParticipantDTO.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .username(p.getUser().getUsername())
                .status(p.getStatus())
                .stakeEscrowed(p.isStakeEscrowed())
                .amountWon(p.getAmountWon())
                .amountLost(p.getAmountLost())
                .quizScore(p.getQuizScore())
                .joinedAt(p.getJoinedAt())
                .settledAt(p.getSettledAt())
                .build();
    }

    private WagerOutcomeDTO mapToDTO(WagerOutcome o) {
        return WagerOutcomeDTO.builder()
                .id(o.getId())
                .wagerId(o.getWager().getId())
                .winnerId(o.getWinner() != null ? o.getWinner().getId() : null)
                .winnerUsername(o.getWinner() != null ? o.getWinner().getUsername() : null)
                .loserId(o.getLoser() != null ? o.getLoser().getId() : null)
                .loserUsername(o.getLoser() != null ? o.getLoser().getUsername() : null)
                .settlementType(o.getSettlementType())
                .amountDistributed(o.getAmountDistributed())
                .penaltyAssigned(o.isPenaltyAssigned())
                .notes(o.getNotes())
                .settledAt(o.getSettledAt())
                .build();
    }
}
