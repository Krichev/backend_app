package com.my.challenger.web.controllers;

import com.my.challenger.dto.challenge.ChallengeAudioConfigDTO;
import com.my.challenger.dto.challenge.ChallengeAudioResponseDTO;
import com.my.challenger.service.ChallengeAudioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Challenge Audio Management", description = "API for managing audio configuration in challenges")
public class ChallengeAudioController {

    private final ChallengeAudioService challengeAudioService;

    @PutMapping("/{challengeId}/audio-config")
    @Operation(summary = "Update challenge audio configuration")
    public ResponseEntity<ChallengeAudioResponseDTO> updateChallengeAudioConfig(
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeAudioConfigDTO config,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("🎵 Updating audio config for challenge {}, user: {}", challengeId, userDetails.getUsername());
        ChallengeAudioResponseDTO response = challengeAudioService.updateAudioConfig(challengeId, config);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{challengeId}/audio-config")
    @Operation(summary = "Get challenge audio configuration")
    public ResponseEntity<ChallengeAudioResponseDTO> getChallengeAudioConfig(
            @PathVariable Long challengeId) {
        
        log.info("📖 Getting audio config for challenge {}", challengeId);
        ChallengeAudioResponseDTO response = challengeAudioService.getAudioConfig(challengeId);
        if (response == null) {
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{challengeId}/audio-config")
    @Operation(summary = "Remove challenge audio configuration")
    public ResponseEntity<Void> removeChallengeAudioConfig(
            @PathVariable Long challengeId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("🗑️ Removing audio config for challenge {}, user: {}", challengeId, userDetails.getUsername());
        challengeAudioService.removeAudioConfig(challengeId);
        return ResponseEntity.noContent().build();
    }
}