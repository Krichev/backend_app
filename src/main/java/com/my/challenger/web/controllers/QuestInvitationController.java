package com.my.challenger.web.controllers;

import com.my.challenger.dto.MessageResponse;
import com.my.challenger.dto.invitation.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.QuestInvitationStatus;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.QuestInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/invitations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Quest Invitations", description = "Quest invitation management with stake negotiation")
public class QuestInvitationController {

    private final QuestInvitationService invitationService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create quest invitation", 
               description = "Invite a user to join a quest with an optional stake")
    public ResponseEntity<QuestInvitationDTO> createInvitation(
            @Valid @RequestBody CreateQuestInvitationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(invitationService.createInvitation(request, user.getId()));
    }

    @GetMapping("/received")
    @Operation(summary = "Get received invitations")
    public ResponseEntity<List<InvitationSummaryDTO>> getReceivedInvitations(
            @RequestParam(required = false) List<QuestInvitationStatus> statuses,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        List<QuestInvitationStatus> statusFilter = statuses != null ? statuses : 
                List.of(QuestInvitationStatus.PENDING, QuestInvitationStatus.NEGOTIATING);
        return ResponseEntity.ok(invitationService.getReceivedInvitations(user.getId(), statusFilter));
    }

    @GetMapping("/sent")
    @Operation(summary = "Get sent invitations")
    public ResponseEntity<List<InvitationSummaryDTO>> getSentInvitations(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(invitationService.getSentInvitations(user.getId()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get invitation details")
    public ResponseEntity<QuestInvitationDTO> getInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(invitationService.getInvitation(id, user.getId()));
    }

    @PostMapping("/{id}/respond")
    @Operation(summary = "Respond to invitation", 
               description = "Accept or decline an invitation")
    public ResponseEntity<QuestInvitationDTO> respondToInvitation(
            @PathVariable Long id,
            @Valid @RequestBody RespondToInvitationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(invitationService.respondToInvitation(id, request, user.getId()));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Cancel invitation", description = "Cancel a pending invitation (inviter only)")
    public ResponseEntity<MessageResponse> cancelInvitation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        invitationService.cancelInvitation(id, user.getId());
        return ResponseEntity.ok(new MessageResponse("Invitation cancelled successfully"));
    }

    @PostMapping("/{id}/counter-offer")
    @Operation(summary = "Create counter-offer", 
               description = "Propose different stake terms (invitee only)")
    public ResponseEntity<QuestInvitationDTO> createCounterOffer(
            @PathVariable Long id,
            @Valid @RequestBody CreateCounterOfferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(invitationService.createCounterOffer(id, request, user.getId()));
    }

    @PostMapping("/{id}/negotiations/{negotiationId}/respond")
    @Operation(summary = "Respond to counter-offer", 
               description = "Accept or reject a counter-offer (inviter only)")
    public ResponseEntity<QuestInvitationDTO> respondToCounterOffer(
            @PathVariable Long id,
            @PathVariable Long negotiationId,
            @Valid @RequestBody RespondToCounterOfferRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(
                invitationService.respondToCounterOffer(id, negotiationId, request, user.getId()));
    }

    @GetMapping("/can-invite/{targetUserId}")
    @Operation(summary = "Check if can invite user")
    public ResponseEntity<Map<String, Boolean>> canInviteUser(
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        boolean canInvite = invitationService.canInviteUser(user.getId(), targetUserId);
        return ResponseEntity.ok(Map.of("canInvite", canInvite));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
