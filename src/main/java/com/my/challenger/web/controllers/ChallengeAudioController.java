package com.my.challenger.web.controllers;

import com.my.challenger.dto.challenge.ChallengeAudioConfigDTO;
import com.my.challenger.dto.challenge.ChallengeAudioResponseDTO;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.ChallengeAudioService;
import com.my.challenger.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/challenges")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Challenge Audio Management", description = "API for managing audio configuration in challenges")
public class ChallengeAudioController {

    private final ChallengeAudioService challengeAudioService;
    private final UserRepository userRepository;
    private final MediaService mediaService;

    @PutMapping("/{challengeId}/audio-config")
    @Operation(summary = "Update challenge audio configuration")
    public ResponseEntity<ChallengeAudioResponseDTO> updateChallengeAudioConfig(
            @PathVariable Long challengeId,
            @Valid @RequestBody ChallengeAudioConfigDTO config,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        log.info("🎵 Updating audio config for challenge {}, user: {}", challengeId, userDetails.getUsername());
        Long userId = getUserId(userDetails);
        ChallengeAudioResponseDTO response = challengeAudioService.updateAudioConfig(challengeId, config, userId);
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
        Long userId = getUserId(userDetails);
        challengeAudioService.removeAudioConfig(challengeId, userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{challengeId}/audio")
    @Operation(summary = "Upload audio file for challenge")
    public ResponseEntity<Map<String, Object>> uploadChallengeAudio(
            @PathVariable Long challengeId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("📤 Uploading audio for challenge {}, user: {}", challengeId, userDetails.getUsername());
        Long userId = getUserId(userDetails);
        // Authorization is handled in service layer or we can do a quick check here if challengeService was available
        // But following the pattern, we should probably move auth to mediaService.uploadChallengeAudio if possible
        // For now, I'll just migrate the endpoint.
        return ResponseEntity.ok(mediaService.uploadChallengeAudio(challengeId, file, userId));
    }

    private Long getUserId(UserDetails userDetails) {
        if (userDetails == null) {
            throw new IllegalArgumentException("User not authenticated");
        }
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"))
                .getId();
    }
}