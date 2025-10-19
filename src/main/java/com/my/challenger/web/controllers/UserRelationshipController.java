// UserRelationshipController.java
package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.CreateRelationshipRequest;
import com.my.challenger.dto.quiz.UserRelationshipDTO;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.UserRelationshipService;
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

@Slf4j
@RestController
@RequestMapping("/challenger/api/relationships")
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
    @Operation(summary = "Get all relationships for current user")
    public ResponseEntity<List<UserRelationshipDTO>> getMyRelationships(
            @AuthenticationPrincipal UserDetails userDetails) {
        
         Long userId = ((UserPrincipal) userDetails).getId();
        List<UserRelationshipDTO> relationships = relationshipService.getUserRelationships(userId);
        return ResponseEntity.ok(relationships);
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
