package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.penalty.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.PenaltyStatus;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.PenaltyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/penalties")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Penalty Management", description = "Endpoints for managing penalties and punishments")
public class PenaltyController {

    private final PenaltyService penaltyService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    @Operation(summary = "Get my penalties")
    public ResponseEntity<Page<PenaltyDTO>> getMyPenalties(
            @RequestParam(required = false) PenaltyStatus status,
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.getMyPenalties(user.getId(), status, pageable));
    }

    @GetMapping("/my/summary")
    @Operation(summary = "Get summary of my penalties")
    public ResponseEntity<PenaltySummaryDTO> getMyPenaltySummary(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.getPenaltySummary(user.getId()));
    }

    @GetMapping("/to-review")
    @Operation(summary = "Get penalties waiting for my review")
    public ResponseEntity<List<PenaltyDTO>> getPenaltiesToReview(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.getPenaltiesToReview(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get penalty details")
    public ResponseEntity<PenaltyDTO> getPenalty(@PathVariable Long id) {
        return ResponseEntity.ok(penaltyService.getPenaltyById(id));
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Mark penalty as in progress")
    public ResponseEntity<PenaltyDTO> startPenalty(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.startPenalty(id, user.getId()));
    }

    @PostMapping(value = "/{id}/submit-proof", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Submit proof for a penalty")
    public ResponseEntity<PenaltyDTO> submitProof(
            @PathVariable Long id,
            @RequestParam(required = false) String description,
            @RequestPart(value = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.submitProof(id, user.getId(), description, file));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify a penalty completion")
    public ResponseEntity<PenaltyDTO> verifyPenalty(
            @PathVariable Long id,
            @RequestBody VerifyPenaltyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.verifyPenalty(id, user.getId(), request.isApproved(), request.getNotes()));
    }

    @PostMapping("/{id}/appeal")
    @Operation(summary = "Appeal a penalty")
    public ResponseEntity<PenaltyDTO> appealPenalty(
            @PathVariable Long id,
            @RequestBody AppealPenaltyRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.appealPenalty(id, user.getId(), request));
    }

    @PostMapping("/{id}/waive")
    @Operation(summary = "Waive a penalty (creator/admin only)")
    public ResponseEntity<PenaltyDTO> waivePenalty(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(penaltyService.waivePenalty(id, user.getId()));
    }

    @GetMapping("/challenge/{challengeId}")
    @Operation(summary = "Get all penalties for a challenge")
    public ResponseEntity<List<PenaltyDTO>> getPenaltiesByChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(penaltyService.getPenaltiesByChallenge(challengeId));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));
    }
}
