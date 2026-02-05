package com.my.challenger.service;

import com.my.challenger.dto.competitive.*;

import java.util.List;
import java.util.Map;

public interface CompetitiveMatchService {
    
    // Match Creation
    CompetitiveMatchDTO createFriendChallenge(Long challengerId, CreateFriendChallengeRequest request);
    MatchmakingStatusDTO joinMatchmaking(Long userId, JoinMatchmakingRequest request);
    void cancelMatchmaking(Long userId);
    MatchmakingStatusDTO getMatchmakingStatus(Long userId);

    // Invitations
    List<CompetitiveMatchInvitationDTO> getPendingInvitations(Long userId);
    CompetitiveMatchDTO respondToInvitation(Long userId, RespondToInvitationRequest request);

    // Match Lifecycle
    CompetitiveMatchDTO getMatch(Long matchId, Long userId);
    CompetitiveMatchDTO startMatch(Long matchId, Long userId);
    CompetitiveMatchRoundDTO startRound(Long matchId, Long userId);
    CompetitiveMatchRoundDTO submitPerformance(Long userId, SubmitRoundPerformanceRequest request);
    CompetitiveMatchDTO cancelMatch(Long matchId, Long userId, String reason);

    // Queries  
    List<CompetitiveMatchSummaryDTO> getUserMatches(Long userId, String status, int page, int size);
    List<CompetitiveMatchSummaryDTO> getActiveMatches(Long userId);
    MatchResultDTO getMatchResult(Long matchId, Long userId);

    // Statistics
    Map<String, Object> getUserCompetitiveStats(Long userId);
}
