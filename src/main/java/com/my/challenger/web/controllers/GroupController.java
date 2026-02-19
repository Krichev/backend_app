package com.my.challenger.web.controllers;

import com.my.challenger.dto.GroupResponseDTO;
import com.my.challenger.dto.MessageResponse;
import com.my.challenger.entity.User;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.impl.GroupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/groups")
@RequiredArgsConstructor
@Slf4j
public class GroupController {
    private final GroupService groupService;
    private final UserRepository userRepository;
    
    /**
     * Get groups for the currently authenticated user
     */
    @GetMapping("/me")
    public ResponseEntity<List<GroupResponseDTO>> getCurrentUserGroups(
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        log.info("Fetching groups for user: {}", user.getId());
        List<GroupResponseDTO> groups = groupService.getUserGroups(user.getId());
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Get groups for a specific user by ID
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<GroupResponseDTO>> getUserGroups(@PathVariable Long userId) {
        log.info("Fetching groups for user ID: {}", userId);
        List<GroupResponseDTO> groups = groupService.getUserGroups(userId);
        return ResponseEntity.ok(groups);
    }
    
    /**
     * Join a group
     */
    @PostMapping("/{groupId}/join")
    public ResponseEntity<?> joinGroup(
            @PathVariable Long groupId,
            @AuthenticationPrincipal UserDetails userDetails) {
        
        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        try {
            groupService.joinGroup(groupId, user.getId());
            return ResponseEntity.ok(new MessageResponse("Successfully joined group"));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(new MessageResponse(e.getMessage()));
        }
    }
}