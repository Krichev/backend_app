package com.my.challenger.web.controllers;

import com.my.challenger.dto.lock.*;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.UnlockRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/unlock-requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Unlock Request Management", description = "Endpoints for requesting and approving device unlocks")
public class UnlockRequestController {

    private final UnlockRequestService unlockRequestService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create an unlock request")
    public ResponseEntity<UnlockRequestDTO> createRequest(
            @RequestBody CreateUnlockRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return new ResponseEntity<>(unlockRequestService.createUnlockRequest(user.getId(), request), HttpStatus.CREATED);
    }

    @GetMapping("/my/pending")
    @Operation(summary = "Get my pending unlock requests")
    public ResponseEntity<List<UnlockRequestDTO>> getMyPendingRequests(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.getMyPendingRequests(user.getId()));
    }

    @GetMapping("/to-approve")
    @Operation(summary = "Get unlock requests awaiting my approval")
    public ResponseEntity<List<UnlockRequestDTO>> getRequestsToApprove(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.getRequestsToApprove(user.getId()));
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve an unlock request")
    public ResponseEntity<UnlockRequestDTO> approveRequest(
            @PathVariable Long id,
            @RequestBody ApproveUnlockRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.approveUnlockRequest(user.getId(), id, request));
    }

    @PostMapping("/{id}/deny")
    @Operation(summary = "Deny an unlock request")
    public ResponseEntity<UnlockRequestDTO> denyRequest(
            @PathVariable Long id,
            @RequestBody DenyUnlockRequestDTO request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.denyUnlockRequest(user.getId(), id, request));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel own pending request")
    public ResponseEntity<UnlockRequestDTO> cancelRequest(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.cancelUnlockRequest(user.getId(), id));
    }

    @PostMapping("/emergency-bypass")
    @Operation(summary = "Use emergency bypass")
    public ResponseEntity<UnlockRequestDTO> useEmergencyBypass(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.useEmergencyBypass(user.getId()));
    }

    @PostMapping("/pay-penalty")
    @Operation(summary = "Self-unlock by paying penalty cost")
    public ResponseEntity<UnlockRequestDTO> payPenalty(
            @RequestParam Long penaltyId,
            @RequestParam String paymentType,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.payPenaltyToUnlock(user.getId(), paymentType, penaltyId));
    }

    @GetMapping("/config")
    @Operation(summary = "Get my lock config")
    public ResponseEntity<AccountLockConfigDTO> getMyConfig(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.getMyLockConfig(user.getId()));
    }

    @PutMapping("/config")
    @Operation(summary = "Update my lock config")
    public ResponseEntity<AccountLockConfigDTO> updateMyConfig(
            @RequestBody AccountLockConfigDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.updateMyLockConfig(user.getId(), dto));
    }

    @GetMapping("/config/{userId}")
    @Operation(summary = "Get child's lock config")
    public ResponseEntity<AccountLockConfigDTO> getChildConfig(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.getChildLockConfig(user.getId(), userId));
    }

    @PutMapping("/config/{userId}")
    @Operation(summary = "Update child's lock config")
    public ResponseEntity<AccountLockConfigDTO> updateChildConfig(
            @PathVariable Long userId,
            @RequestBody AccountLockConfigDTO dto,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(unlockRequestService.updateChildLockConfig(user.getId(), userId, dto));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));
    }
}
