package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.wager.*;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.WagerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wagers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wager Management", description = "Endpoints for creating and managing wagers")
public class WagerController {

    private final WagerService wagerService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create a new wager")
    public ResponseEntity<WagerDTO> createWager(
            @RequestBody CreateWagerRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(wagerService.createWager(request, user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wager details")
    public ResponseEntity<WagerDTO> getWager(@PathVariable Long id) {
        return ResponseEntity.ok(wagerService.getWagerById(id));
    }

    @PostMapping("/{id}/accept")
    @Operation(summary = "Accept a wager invitation")
    public ResponseEntity<WagerDTO> acceptWager(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(wagerService.acceptWager(id, user.getId()));
    }

    @PostMapping("/{id}/decline")
    @Operation(summary = "Decline a wager invitation")
    public ResponseEntity<MessageResponse> declineWager(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        wagerService.declineWager(id, user.getId());
        return ResponseEntity.ok(new MessageResponse("Wager declined successfully"));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a wager")
    public ResponseEntity<MessageResponse> cancelWager(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        wagerService.cancelWager(id, user.getId());
        return ResponseEntity.ok(new MessageResponse("Wager cancelled successfully"));
    }

    @PostMapping("/{id}/settle")
    @Operation(summary = "Manually trigger wager settlement")
    public ResponseEntity<WagerOutcomeDTO> settleWager(@PathVariable Long id) {
        return ResponseEntity.ok(wagerService.settleWager(id));
    }

    @GetMapping("/challenge/{challengeId}")
    @Operation(summary = "List wagers for a challenge")
    public ResponseEntity<List<WagerDTO>> getWagersByChallenge(@PathVariable Long challengeId) {
        return ResponseEntity.ok(wagerService.getWagersByChallenge(challengeId));
    }

    @GetMapping("/my/active")
    @Operation(summary = "Get current user's active wagers")
    public ResponseEntity<List<WagerDTO>> getMyActiveWagers(@AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(wagerService.getActiveWagersByUser(user.getId()));
    }

    @GetMapping("/my/history")
    @Operation(summary = "Get current user's wager history")
    public ResponseEntity<Page<WagerDTO>> getMyWagerHistory(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(wagerService.getWagerHistoryByUser(user.getId(), pageable));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userDetails.getUsername()));
    }
}
