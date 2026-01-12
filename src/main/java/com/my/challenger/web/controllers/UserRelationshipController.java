// UserRelationshipController.java
package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.*;
import com.my.challenger.entity.enums.RelationshipStatus;
import com.my.challenger.entity.enums.RelationshipType;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.UserRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/relationships")
@RequiredArgsConstructor
@Tag(name = "User Relationships", description = "Manage user relationships (friends/family)")
public class UserRelationshipController {

    private final UserRelationshipService relationshipService;

    @PostMapping
    @Operation(summary = "Create a relationship request")
    public ResponseEntity<UserRelationshipDTO> createRelationship(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateRelationshipRequest request) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        UserRelationshipDTO relationship = relationshipService.createRelationshipRequest(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(relationship);
    }

    @GetMapping
    @Operation(summary = "Get relationships for current user with filtering, sorting and pagination")
    public ResponseEntity<Page<UserRelationshipDTO>> getRelationships(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(required = false) Long relatedUserId,
            @RequestParam(required = false) RelationshipType type,
            @RequestParam(required = false, defaultValue = "ACCEPTED") RelationshipStatus status,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        Page<UserRelationshipDTO> relationships = relationshipService.getRelationshipsFiltered(userId, relatedUserId, type, status, sort, page, size);
        return ResponseEntity.ok(relationships);
    }

    @PutMapping("/{relationshipId}")
    @Operation(summary = "Update relationship details (nickname, notes, type, favorite)")
    public ResponseEntity<UserRelationshipDTO> updateRelationship(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long relationshipId,
            @RequestBody UpdateRelationshipRequest request) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        UserRelationshipDTO relationship = relationshipService.updateRelationship(userId, relationshipId, request);
        return ResponseEntity.ok(relationship);
    }

    @PutMapping("/{relationshipId}/favorite")
    @Operation(summary = "Toggle favorite status")
    public ResponseEntity<UserRelationshipDTO> toggleFavorite(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long relationshipId) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        UserRelationshipDTO relationship = relationshipService.toggleFavorite(userId, relationshipId);
        return ResponseEntity.ok(relationship);
    }

    @GetMapping("/suggestions")
    @Operation(summary = "Get suggested connections based on mutual friends")
    public ResponseEntity<List<UserSuggestionDTO>> getSuggestions(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(relationshipService.getSuggestions(userId));
    }

    @GetMapping("/mutual/{otherUserId}")
    @Operation(summary = "Get mutual connections with another user")
    public ResponseEntity<List<MutualConnectionDTO>> getMutualConnections(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long otherUserId) {
        
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(relationshipService.getMutualConnections(userId, otherUserId));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending relationship requests")
    public ResponseEntity<List<UserRelationshipDTO>> getPendingRequests(
            @AuthenticationPrincipal UserDetails userDetails) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        List<UserRelationshipDTO> requests = relationshipService.getPendingRequests(userId);
        return ResponseEntity.ok(requests);
    }

    @PutMapping("/{relationshipId}/accept")
    @Operation(summary = "Accept a relationship request")
    public ResponseEntity<UserRelationshipDTO> acceptRelationship(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long relationshipId) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        UserRelationshipDTO relationship = relationshipService.acceptRelationship(userId, relationshipId);
        return ResponseEntity.ok(relationship);
    }

    @PutMapping("/{relationshipId}/reject")
    @Operation(summary = "Reject a relationship request")
    public ResponseEntity<Void> rejectRelationship(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long relationshipId) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        relationshipService.rejectRelationship(userId, relationshipId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{relationshipId}")
    @Operation(summary = "Remove a relationship")
    public ResponseEntity<Void> removeRelationship(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long relationshipId) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        relationshipService.removeRelationship(userId, relationshipId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/check/{otherUserId}")
    @Operation(summary = "Check if connected with another user")
    public ResponseEntity<Boolean> checkConnection(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long otherUserId) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        boolean connected = relationshipService.areUsersConnected(userId, otherUserId);
        return ResponseEntity.ok(connected);
    }
}
