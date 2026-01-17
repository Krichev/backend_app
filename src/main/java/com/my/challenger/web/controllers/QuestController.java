package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.ChallengeDTO;
import com.my.challenger.dto.quest.QuestAudioConfigDTO;
import com.my.challenger.dto.quest.QuestAudioResponseDTO;
import com.my.challenger.entity.User;
import com.my.challenger.exception.InvalidAudioSegmentException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.ChallengeAudioService;
import com.my.challenger.service.MediaService;
import com.my.challenger.service.impl.ChallengeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * @deprecated Quest endpoints are being consolidated into ChallengeController.
 * Use /api/challenges endpoints instead.
 * This controller now only provides legacy audio configuration endpoints
 * that redirect to challenge endpoints.
 */
@RestController
@RequestMapping("/api/quests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quest Management (Deprecated)",
     description = "⚠️ DEPRECATED: Use /api/challenges endpoints instead. Quest functionality has been consolidated into Challenge.")
public class QuestController {

    private final ChallengeService challengeService;
    private final ChallengeAudioService challengeAudioService;
    private final MediaService mediaService;
    private final UserRepository userRepository;

    /**
     * @deprecated Use DELETE /api/challenges/{id} instead
     * This endpoint attempts to find a challenge linked to the quest and delete it
     */
    @DeleteMapping("/{questId}")
    @Operation(summary = "Delete quest (DEPRECATED)", 
               description = "⚠️ DEPRECATED: Use DELETE /api/challenges/{id} instead")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Quest deleted successfully (via challenge endpoint)"),
            @ApiResponse(responseCode = "403", description = "Forbidden - only creator can delete"),
            @ApiResponse(responseCode = "404", description = "Quest not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<MessageResponse> deleteQuest(
            @Parameter(description = "Quest ID", required = true)
            @PathVariable Long questId,
            @AuthenticationPrincipal UserDetails userDetails) {

        log.warn("⚠️ Deprecated endpoint called: DELETE /api/quests/{}. Use /api/challenges/{} instead.", 
                 questId, questId);

        User user = getUserFromUserDetails(userDetails);

        try {
            // Validate and delete using challenge service (assuming questId == challengeId)
            challengeService.validateChallengeOwnership(questId, user.getId());
            challengeService.deleteChallenge(questId);
            return ResponseEntity.ok(new MessageResponse("Quest deleted successfully (via challenge endpoint)"));
        } catch (ResourceNotFoundException e) {
            log.error("❌ Quest/Challenge not found: {}", questId);
            throw new ResourceNotFoundException("Quest not found with ID: " + questId + 
                ". Note: Quest endpoints are deprecated. Use /api/challenges instead.");
        }
    }

    @GetMapping("/{questId}")
    @Operation(summary = "Get quest by ID (DEPRECATED)")
    public ResponseEntity<ChallengeDTO> getQuest(
            @PathVariable Long questId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.warn("⚠️ Deprecated endpoint called: GET /api/quests/{}. Use /api/challenges/{} instead.", 
                 questId, questId);
        
        User user = getUserFromUserDetails(userDetails);
        ChallengeDTO challenge = challengeService.getChallengeById(questId, user.getId());
        return ResponseEntity.ok(challenge);
    }

    private User getUserFromUserDetails(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}