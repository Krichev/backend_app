package com.my.challenger.web.controllers;

import com.my.challenger.dto.quiz.ContactGroupDTO;
import com.my.challenger.dto.quiz.CreateContactGroupRequest;
import com.my.challenger.dto.quiz.UpdateContactGroupRequest;
import com.my.challenger.security.UserPrincipal;
import com.my.challenger.service.impl.ContactGroupService;
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
@RequestMapping("/api/contact-groups")
@RequiredArgsConstructor
@Tag(name = "Contact Groups", description = "Manage custom groups for contacts")
public class ContactGroupController {

    private final ContactGroupService groupService;

    @GetMapping
    @Operation(summary = "Get all contact groups for current user")
    public ResponseEntity<List<ContactGroupDTO>> getMyGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(groupService.getUserGroups(userId));
    }

    @PostMapping
    @Operation(summary = "Create a new contact group")
    public ResponseEntity<ContactGroupDTO> createGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateContactGroupRequest request) {
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(userId, request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a contact group")
    public ResponseEntity<ContactGroupDTO> updateGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody UpdateContactGroupRequest request) {
        Long userId = ((UserPrincipal) userDetails).getId();
        return ResponseEntity.ok(groupService.updateGroup(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a contact group")
    public ResponseEntity<Void> deleteGroup(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        Long userId = ((UserPrincipal) userDetails).getId();
        groupService.deleteGroup(userId, id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/members")
    @Operation(summary = "Add members to a contact group")
    public ResponseEntity<Void> addMembers(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody List<Long> relationshipIds) {
        Long userId = ((UserPrincipal) userDetails).getId();
        groupService.addMembers(userId, id, relationshipIds);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{groupId}/members/{relationshipId}")
    @Operation(summary = "Remove a member from a contact group")
    public ResponseEntity<Void> removeMember(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long groupId,
            @PathVariable Long relationshipId) {
        Long userId = ((UserPrincipal) userDetails).getId();
        groupService.removeMember(userId, groupId, relationshipId);
        return ResponseEntity.noContent().build();
    }
}
