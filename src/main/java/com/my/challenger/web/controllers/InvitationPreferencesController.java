package com.my.challenger.web.controllers;

import com.my.challenger.dto.invitation.UpdateInvitationPreferencesRequest;
import com.my.challenger.dto.invitation.UserInvitationPreferencesDTO;
import com.my.challenger.entity.User;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.InvitationPrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me/invitation-preferences")
@RequiredArgsConstructor
@Tag(name = "Invitation Preferences", description = "User invitation privacy settings")
public class InvitationPreferencesController {

    private final InvitationPrivacyService privacyService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Get invitation preferences")
    public ResponseEntity<UserInvitationPreferencesDTO> getPreferences(
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(privacyService.getPreferences(user.getId()));
    }

    @PutMapping
    @Operation(summary = "Update invitation preferences")
    public ResponseEntity<UserInvitationPreferencesDTO> updatePreferences(
            @Valid @RequestBody UpdateInvitationPreferencesRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User user = getUser(userDetails);
        return ResponseEntity.ok(privacyService.updatePreferences(user.getId(), request));
    }

    private User getUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
