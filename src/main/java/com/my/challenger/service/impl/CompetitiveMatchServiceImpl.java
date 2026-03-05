package com.my.challenger.service.impl;

import com.my.challenger.config.StorageProperties;
import com.my.challenger.dto.audio.QuestionResponseDTO;
import com.my.challenger.dto.competitive.*;
import com.my.challenger.dto.wager.WagerDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.competitive.CompetitiveMatch;
import com.my.challenger.entity.competitive.CompetitiveMatchInvitation;
import com.my.challenger.entity.competitive.CompetitiveMatchRound;
import com.my.challenger.entity.competitive.MatchmakingQueueEntry;
import com.my.challenger.entity.enums.*;
import com.my.challenger.entity.quiz.QuizQuestion;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.*;
import com.my.challenger.service.CompetitiveMatchService;
import com.my.challenger.service.WagerService;
import com.my.challenger.service.integration.KaraokeScoringClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class CompetitiveMatchServiceImpl implements CompetitiveMatchService {

    private final CompetitiveMatchRepository matchRepository;
    private final CompetitiveMatchRoundRepository roundRepository;
    private final CompetitiveMatchInvitationRepository invitationRepository;
    private final MatchmakingQueueRepository matchmakingQueueRepository;
    private final UserRepository userRepository;
    private final QuizQuestionRepository questionRepository;
    private final UserRelationshipService userRelationshipService;
    private final WagerService wagerService;
    private final KaraokeScoringClient karaokeClient;
    private final MatchmakingService matchmakingService;
    private final StorageProperties storageProperties;

    // ==================================================================================
    // MATCH CREATION
    // ==================================================================================

    @Override
    @Transactional
    public CompetitiveMatchDTO createFriendChallenge(Long challengerId, CreateFriendChallengeRequest request) {
        log.info("🎮 User {} challenging friend {}", challengerId, request.getInviteeId());

        User challenger = getUser(challengerId);
        User invitee = getUser(request.getInviteeId());

        // Verify friendship
        if (!userRelationshipService.areUsersConnected(challengerId, request.getInviteeId())) {
            throw new IllegalStateException("Users must be connected to challenge each other");
        }

        // Create match
        CompetitiveMatch match = CompetitiveMatch.builder()
                .matchType(CompetitiveMatchType.FRIEND_CHALLENGE)
                .status(CompetitiveMatchStatus.WAITING_FOR_OPPONENT)
                .player1(challenger)
                .player2(invitee) // Set player2 initially as the target
                .totalRounds(request.getTotalRounds())
                .currentRound(0)
                .audioChallengeType(request.getAudioChallengeType())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        // Link Wager if provided
        if (request.getWagerId() != null) {
            // Check wager exists and is valid
        }

        match = matchRepository.save(match);

        // Create Invitation
        CompetitiveMatchInvitation invitation = CompetitiveMatchInvitation.builder()
                .match(match)
                .inviter(challenger)
                .invitee(invitee)
                .status("PENDING")
                .message(request.getMessage())
                .expiresAt(match.getExpiresAt())
                .build();

        invitationRepository.save(invitation);

        return convertToDTO(match);
    }

    @Override
    @Transactional
    public MatchmakingStatusDTO joinMatchmaking(Long userId, JoinMatchmakingRequest request) {
        log.info("🎮 User {} joining matchmaking queue for {}", userId, request.getAudioChallengeType());

        User user = getUser(userId);

        // Check if already in queue
        Optional<MatchmakingQueueEntry> existing = matchmakingQueueRepository.findByUserId(userId);
        if (existing.isPresent()) {
            MatchmakingQueueEntry entry = existing.get();
            if (entry.getStatus() == MatchmakingStatus.QUEUED) {
                log.info("User {} already in queue, returning status", userId);
                return matchmakingService.getQueueStatus(userId);
            }
            matchmakingQueueRepository.delete(entry);
            matchmakingQueueRepository.flush();
        }
        
        MatchmakingQueueEntry entry = MatchmakingQueueEntry.builder()
                .user(user)
                .status(MatchmakingStatus.QUEUED)
                .audioChallengeType(request.getAudioChallengeType())
                .preferredRounds(request.getPreferredRounds())
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build();

        matchmakingQueueRepository.save(entry);

        return matchmakingService.getQueueStatus(userId);
    }

    @Override
    @Transactional
    public void cancelMatchmaking(Long userId) {
        log.info("User {} cancelling matchmaking", userId);
        Optional<MatchmakingQueueEntry> entry = matchmakingQueueRepository.findByUserId(userId);
        entry.ifPresent(e -> {
            e.setStatus(MatchmakingStatus.CANCELLED);
            matchmakingQueueRepository.save(e);
            matchmakingQueueRepository.delete(e);
        });
    }

    @Override
    public MatchmakingStatusDTO getMatchmakingStatus(Long userId) {
        return matchmakingService.getQueueStatus(userId);
    }

    // ==================================================================================
    // INVITATIONS
    // ==================================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CompetitiveMatchInvitationDTO> getPendingInvitations(Long userId) {
        return invitationRepository.findByInviteeIdAndStatus(userId, "PENDING").stream()
                .map(this::convertInvitationToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CompetitiveMatchDTO respondToInvitation(Long userId, RespondToInvitationRequest request) {
        log.info("User {} responding to invitation {}", userId, request.getInvitationId());

        CompetitiveMatchInvitation invitation = invitationRepository.findById(request.getInvitationId())
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (!invitation.getInvitee().getId().equals(userId)) {
            throw new IllegalArgumentException("Not authorized to respond to this invitation");
        }

        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalStateException("Invitation is not pending");
        }

        CompetitiveMatch match = invitation.getMatch();

        if (request.getAccepted()) {
            invitation.setStatus("ACCEPTED");
            match.setStatus(CompetitiveMatchStatus.READY);
            match.setPlayer2(getUser(userId));
        } else {
            invitation.setStatus("DECLINED");
            match.setStatus(CompetitiveMatchStatus.CANCELLED);
        }

        invitation.setRespondedAt(LocalDateTime.now());
        invitationRepository.save(invitation);
        matchRepository.save(match);

        return convertToDTO(match);
    }

    // ==================================================================================
    // MATCH LIFECYCLE
    // ==================================================================================

    @Override
    @Transactional(readOnly = true)
    public CompetitiveMatchDTO getMatch(Long matchId, Long userId) {
        CompetitiveMatch match = getMatchEntity(matchId);
        validateParticipant(match, userId);
        return convertToDTO(match);
    }

    @Override
    @Transactional
    public CompetitiveMatchDTO startMatch(Long matchId, Long userId) {
        log.info("Starting match {} by user {}", matchId, userId);
        
        CompetitiveMatch match = getMatchEntity(matchId);
        validateParticipant(match, userId);

        if (match.getStatus() == CompetitiveMatchStatus.IN_PROGRESS) {
            log.info("Match {} already in progress", matchId);
            return convertToDTO(match);
        }

        if (match.getStatus() != CompetitiveMatchStatus.READY) {
            throw new IllegalStateException("Match is not ready to start. Status: " + match.getStatus());
        }

        match.setStatus(CompetitiveMatchStatus.IN_PROGRESS);
        match.setStartedAt(LocalDateTime.now());
        match.setCurrentRound(1);
        
        match = matchRepository.save(match);
        
        createRound(match, 1);

        return convertToDTO(match);
    }

    @Override
    @Transactional
    public CompetitiveMatchRoundDTO startRound(Long matchId, Long userId) {
        CompetitiveMatch match = getMatchEntity(matchId);
        validateParticipant(match, userId);
        
        Optional<CompetitiveMatchRound> roundOpt = roundRepository
                .findFirstByMatchIdAndStatusNotOrderByRoundNumberAsc(matchId, CompetitiveRoundStatus.COMPLETED);
        
        if (roundOpt.isEmpty()) {
            throw new IllegalStateException("No active round found");
        }
        
        CompetitiveMatchRound round = roundOpt.get();
        
        if (round.getStatus() == CompetitiveRoundStatus.PENDING) {
             if (match.getPlayer1().getId().equals(userId)) {
                 round.setStatus(CompetitiveRoundStatus.PLAYER1_PERFORMING);
             } else {
                 round.setStatus(CompetitiveRoundStatus.PLAYER2_PERFORMING);
             }
             round.setStartedAt(LocalDateTime.now());
             roundRepository.save(round);
        }
        
        return convertRoundToDTO(round);
    }

    @Override
    @Transactional
    public CompetitiveMatchRoundDTO submitPerformance(Long userId, SubmitRoundPerformanceRequest request) {
        log.info("User {} submitting performance for round {}", userId, request.getRoundId());
        
        CompetitiveMatchRound round = roundRepository.findById(request.getRoundId())
                .orElseThrow(() -> new ResourceNotFoundException("Round not found"));
        
        CompetitiveMatch match = round.getMatch();
        if (!match.getId().equals(request.getMatchId())) {
             throw new IllegalArgumentException("Round does not belong to match");
        }
        
        boolean isPlayer1 = match.getPlayer1().getId().equals(userId);
        
        if (isPlayer1) {
            round.setPlayer1SubmissionPath(request.getAudioFilePath());
            round.setPlayer1SubmittedAt(LocalDateTime.now());
        } else {
            round.setPlayer2SubmissionPath(request.getAudioFilePath());
            round.setPlayer2SubmittedAt(LocalDateTime.now());
        }
        
        roundRepository.save(round);
        
        if (round.getPlayer1SubmissionPath() != null && round.getPlayer2SubmissionPath() != null) {
            scoreRound(round);
        } else {
            round.setStatus(isPlayer1 ? CompetitiveRoundStatus.PLAYER2_PERFORMING : CompetitiveRoundStatus.PLAYER1_PERFORMING); 
            roundRepository.save(round);
        }
        
        return convertRoundToDTO(round);
    }
    
    private void scoreRound(CompetitiveMatchRound round) {
        log.info("Scoring round {}", round.getId());
        round.setStatus(CompetitiveRoundStatus.SCORING);
        
        String audioBucket = storageProperties.getBucketForMediaType(MediaType.AUDIO);
        
        QuizQuestion question = round.getQuestion();
        String refAudioKey = null;
        String refAudioBucket = null;
        if (question.getAudioReferenceMedia() != null) {
            refAudioKey = question.getAudioReferenceMedia().getFilePath();
            refAudioBucket = question.getAudioReferenceMedia().getBucketName() != null
                ? question.getAudioReferenceMedia().getBucketName() : audioBucket;
        }
        
        // Score Player 1
        var result1 = karaokeClient.scoreAudio(
                round.getPlayer1SubmissionPath(),
                audioBucket,
                refAudioKey,
                refAudioBucket,
                round.getMatch().getAudioChallengeType().name(),
                question.getRhythmBpm(),
                question.getRhythmTimeSignature()
        );
        
        round.setPlayer1Score(BigDecimal.valueOf(result1.getOverallScore()));
        round.setPlayer1PitchScore(BigDecimal.valueOf(result1.getPitchScore()));
        round.setPlayer1RhythmScore(BigDecimal.valueOf(result1.getRhythmScore()));
        round.setPlayer1VoiceScore(BigDecimal.valueOf(result1.getVoiceScore()));

        // Score Player 2
        var result2 = karaokeClient.scoreAudio(
                round.getPlayer2SubmissionPath(),
                audioBucket,
                refAudioKey,
                refAudioBucket,
                round.getMatch().getAudioChallengeType().name(),
                question.getRhythmBpm(),
                question.getRhythmTimeSignature()
        );

        round.setPlayer2Score(BigDecimal.valueOf(result2.getOverallScore()));
        round.setPlayer2PitchScore(BigDecimal.valueOf(result2.getPitchScore()));
        round.setPlayer2RhythmScore(BigDecimal.valueOf(result2.getRhythmScore()));
        round.setPlayer2VoiceScore(BigDecimal.valueOf(result2.getVoiceScore()));
        
        // Determine Round Winner
        if (round.getPlayer1Score().compareTo(round.getPlayer2Score()) > 0) {
            round.setRoundWinner(round.getMatch().getPlayer1());
            round.getMatch().setPlayer1RoundsWon(round.getMatch().getPlayer1RoundsWon() + 1);
        } else if (round.getPlayer2Score().compareTo(round.getPlayer1Score()) > 0) {
            round.setRoundWinner(round.getMatch().getPlayer2());
            round.getMatch().setPlayer2RoundsWon(round.getMatch().getPlayer2RoundsWon() + 1);
        }
        
        // Update Totals
        CompetitiveMatch match = round.getMatch();
        match.setPlayer1TotalScore(match.getPlayer1TotalScore().add(round.getPlayer1Score()));
        match.setPlayer2TotalScore(match.getPlayer2TotalScore().add(round.getPlayer2Score()));
        
        round.setStatus(CompetitiveRoundStatus.COMPLETED);
        round.setCompletedAt(LocalDateTime.now());
        roundRepository.save(round);
        
        if (match.getCurrentRound() < match.getTotalRounds()) {
            match.setCurrentRound(match.getCurrentRound() + 1);
            match.setStatus(CompetitiveMatchStatus.ROUND_COMPLETE);
            matchRepository.save(match);
            createRound(match, match.getCurrentRound());
        } else {
            completeMatch(match);
        }
    }
    
    private void completeMatch(CompetitiveMatch match) {
        log.info("Completing match {}", match.getId());
        match.setStatus(CompetitiveMatchStatus.COMPLETED);
        match.setCompletedAt(LocalDateTime.now());
        
        if (match.getPlayer1RoundsWon() > match.getPlayer2RoundsWon()) {
            match.setWinner(match.getPlayer1());
        } else if (match.getPlayer2RoundsWon() > match.getPlayer1RoundsWon()) {
            match.setWinner(match.getPlayer2());
        } else {
            if (match.getPlayer1TotalScore().compareTo(match.getPlayer2TotalScore()) > 0) {
                match.setWinner(match.getPlayer1());
            } else if (match.getPlayer2TotalScore().compareTo(match.getPlayer1TotalScore()) > 0) {
                match.setWinner(match.getPlayer2());
            }
        }
        
        matchRepository.save(match);
    }
    
    private void createRound(CompetitiveMatch match, Integer roundNum) {
        List<QuizQuestion> questions = questionRepository.findByQuestionTypeOrderByCreatedAtDesc(QuestionType.AUDIO, org.springframework.data.domain.Pageable.unpaged());
        if (questions.isEmpty()) {
            log.warn("No audio questions found!");
            return;
        }
        QuizQuestion q = questions.get(roundNum % questions.size());
        
        CompetitiveMatchRound round = CompetitiveMatchRound.builder()
                .match(match)
                .roundNumber(roundNum)
                .status(CompetitiveRoundStatus.PENDING)
                .question(q)
                .build();
        
        roundRepository.save(round);
    }

    @Override
    @Transactional
    public CompetitiveMatchDTO cancelMatch(Long matchId, Long userId, String reason) {
        CompetitiveMatch match = getMatchEntity(matchId);
        validateParticipant(match, userId);
        match.setStatus(CompetitiveMatchStatus.CANCELLED);
        return convertToDTO(matchRepository.save(match));
    }

    // ==================================================================================
    // QUERIES
    // ==================================================================================

    @Override
    @Transactional(readOnly = true)
    public List<CompetitiveMatchSummaryDTO> getUserMatches(Long userId, String status, int page, int size) {
        return matchRepository.findByPlayer1IdOrPlayer2Id(userId).stream()
                .map(this::convertSummaryToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CompetitiveMatchSummaryDTO> getActiveMatches(Long userId) {
        return matchRepository.findActiveMatchesByUserId(userId).stream()
                .map(this::convertSummaryToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public MatchResultDTO getMatchResult(Long matchId, Long userId) {
        CompetitiveMatch match = getMatchEntity(matchId);
        if (match.getStatus() != CompetitiveMatchStatus.COMPLETED) {
            throw new IllegalStateException("Match not completed");
        }
        
        return MatchResultDTO.builder()
                .matchId(match.getId())
                .winnerId(match.getWinner() != null ? match.getWinner().getId() : null)
                .winnerUsername(match.getWinner() != null ? match.getWinner().getUsername() : null)
                .player1TotalScore(match.getPlayer1TotalScore())
                .player2TotalScore(match.getPlayer2TotalScore())
                .player1RoundsWon(match.getPlayer1RoundsWon())
                .player2RoundsWon(match.getPlayer2RoundsWon())
                .completedAt(match.getCompletedAt())
                .build();
    }

    @Override
    public Map<String, Object> getUserCompetitiveStats(Long userId) {
        return Map.of("matchesPlayed", 0, "wins", 0);
    }

    // ==================================================================================
    // HELPERS
    // ==================================================================================

    private User getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    private CompetitiveMatch getMatchEntity(Long id) {
        return matchRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Match not found"));
    }
    
    private void validateParticipant(CompetitiveMatch match, Long userId) {
        if (!match.getPlayer1().getId().equals(userId) && 
            (match.getPlayer2() == null || !match.getPlayer2().getId().equals(userId))) {
            throw new org.springframework.security.access.AccessDeniedException("User is not a participant");
        }
    }
    
    private CompetitiveMatchDTO convertToDTO(CompetitiveMatch match) {
        return CompetitiveMatchDTO.builder()
                .summary(convertSummaryToDTO(match))
                .rounds(match.getRounds().stream().map(this::convertRoundToDTO).collect(Collectors.toList()))
                .build();
    }
    
    private CompetitiveMatchSummaryDTO convertSummaryToDTO(CompetitiveMatch match) {
        return CompetitiveMatchSummaryDTO.builder()
                .id(match.getId())
                .matchType(match.getMatchType().name())
                .status(match.getStatus().name())
                .player1Id(match.getPlayer1().getId())
                .player1Username(match.getPlayer1().getUsername())
                .player2Id(match.getPlayer2() != null ? match.getPlayer2().getId() : null)
                .player2Username(match.getPlayer2() != null ? match.getPlayer2().getUsername() : null)
                .currentRound(match.getCurrentRound())
                .totalRounds(match.getTotalRounds())
                .player1TotalScore(match.getPlayer1TotalScore())
                .player2TotalScore(match.getPlayer2TotalScore())
                .startedAt(match.getStartedAt())
                .build();
    }
    
    private CompetitiveMatchRoundDTO convertRoundToDTO(CompetitiveMatchRound round) {
        return CompetitiveMatchRoundDTO.builder()
                .id(round.getId())
                .matchId(round.getMatch().getId())
                .roundNumber(round.getRoundNumber())
                .status(round.getStatus().name())
                .player1Score(round.getPlayer1Score())
                .player2Score(round.getPlayer2Score())
                .build();
    }

    private CompetitiveMatchInvitationDTO convertInvitationToDTO(CompetitiveMatchInvitation invitation) {
        return CompetitiveMatchInvitationDTO.builder()
                .id(invitation.getId())
                .matchId(invitation.getMatch().getId())
                .inviterId(invitation.getInviter().getId())
                .inviterUsername(invitation.getInviter().getUsername())
                .inviteeId(invitation.getInvitee().getId())
                .status(invitation.getStatus())
                .createdAt(invitation.getCreatedAt())
                .build();
    }
}
