package com.my.challenger.web.controllers;

import com.my.challenger.dto.competitive.*;
import com.my.challenger.entity.MediaFile;
import com.my.challenger.entity.enums.MediaCategory;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.CompetitiveMatchService;
import com.my.challenger.service.impl.MinioMediaStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/competitive")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Competitive Matches", description = "Endpoints for 1v1 Karaoke Battles")
public class CompetitiveMatchController {

    private final CompetitiveMatchService matchService;
    private final MinioMediaStorageService mediaStorageService;

    // ==================================================================================
    // MATCHMAKING & CHALLENGES
    // ==================================================================================

    @PostMapping("/challenges")
    @Operation(summary = "Create friend challenge")
    public ResponseEntity<CompetitiveMatchDTO> createFriendChallenge(
            @Valid @RequestBody CreateFriendChallengeRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.createFriendChallenge(user.getId(), request));
    }

    @PostMapping("/matchmaking/join")
    @Operation(summary = "Join matchmaking queue")
    public ResponseEntity<MatchmakingStatusDTO> joinMatchmaking(
            @Valid @RequestBody JoinMatchmakingRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.joinMatchmaking(user.getId(), request));
    }

    @DeleteMapping("/matchmaking")
    @Operation(summary = "Leave matchmaking queue")
    public ResponseEntity<Void> leaveMatchmaking(@AuthenticationPrincipal UserPrincipal user) {
        matchService.cancelMatchmaking(user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/matchmaking/status")
    @Operation(summary = "Get current matchmaking status")
    public ResponseEntity<MatchmakingStatusDTO> getMatchmakingStatus(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.getMatchmakingStatus(user.getId()));
    }

    // ==================================================================================
    // INVITATIONS
    // ==================================================================================

    @GetMapping("/invitations")
    @Operation(summary = "Get pending invitations")
    public ResponseEntity<List<CompetitiveMatchInvitationDTO>> getPendingInvitations(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.getPendingInvitations(user.getId()));
    }

    @PostMapping("/invitations/{id}/respond")
    @Operation(summary = "Respond to invitation")
    public ResponseEntity<CompetitiveMatchDTO> respondToInvitation(
            @PathVariable Long id,
            @Valid @RequestBody RespondToInvitationRequest request,
            @AuthenticationPrincipal UserPrincipal user) {
        
        if (!id.equals(request.getInvitationId())) {
             request.setInvitationId(id);
        }
        return ResponseEntity.ok(matchService.respondToInvitation(user.getId(), request));
    }

    // ==================================================================================
    // MATCH LIFECYCLE
    // ==================================================================================

    @GetMapping("/matches")
    @Operation(summary = "List user's matches")
    public ResponseEntity<List<CompetitiveMatchSummaryDTO>> getUserMatches(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.getUserMatches(user.getId(), status, page, size));
    }

    @GetMapping("/matches/{id}")
    @Operation(summary = "Get match details")
    public ResponseEntity<CompetitiveMatchDTO> getMatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.getMatch(id, user.getId()));
    }

    @PostMapping("/matches/{id}/start")
    @Operation(summary = "Start match")
    public ResponseEntity<CompetitiveMatchDTO> startMatch(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.startMatch(id, user.getId()));
    }

    @PostMapping("/matches/{id}/rounds/start")
    @Operation(summary = "Start current round")
    public ResponseEntity<CompetitiveMatchRoundDTO> startRound(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.startRound(id, user.getId()));
    }

    @PostMapping(value = "/matches/{id}/rounds/{roundId}/submit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit round performance")
    public ResponseEntity<CompetitiveMatchRoundDTO> submitPerformance(
            @PathVariable Long id,
            @PathVariable Long roundId,
            @Parameter(description = "Audio file") @RequestPart("audio") MultipartFile audioFile,
            @AuthenticationPrincipal UserPrincipal user) {
        
        // 1. Upload file
        MediaFile media = mediaStorageService.storeMedia(audioFile, null, MediaCategory.CHALLENGE_PROOF, user.getId());
        String audioPath = media.getS3Key();
        
        // 2. Submit to service
        SubmitRoundPerformanceRequest request = SubmitRoundPerformanceRequest.builder()
                .matchId(id)
                .roundId(roundId)
                .audioFilePath(audioPath)
                .build();
                
        return ResponseEntity.ok(matchService.submitPerformance(user.getId(), request));
    }

    @PostMapping("/matches/{id}/cancel")
    @Operation(summary = "Cancel match")
    public ResponseEntity<CompetitiveMatchDTO> cancelMatch(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal user) {
        String reason = body != null ? body.get("reason") : "Cancelled by user";
        return ResponseEntity.ok(matchService.cancelMatch(id, user.getId(), reason));
    }

    @GetMapping("/matches/{id}/result")
    @Operation(summary = "Get final match result")
    public ResponseEntity<MatchResultDTO> getMatchResult(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.getMatchResult(id, user.getId()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Get user competitive stats")
    public ResponseEntity<Map<String, Object>> getUserStats(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(matchService.getUserCompetitiveStats(user.getId()));
    }
}
