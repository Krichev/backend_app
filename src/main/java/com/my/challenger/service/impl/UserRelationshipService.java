// UserRelationshipService.java
package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.CreateRelationshipRequest;
import com.my.challenger.dto.quiz.UserRelationshipDTO;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserRelationship;
import com.my.challenger.entity.enums.RelationshipStatus;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.exception.BadRequestException;
import com.my.challenger.repository.UserRelationshipRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserRelationshipService {

    private final UserRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    /**
     * Create a new relationship request
     */
    @Transactional
    public UserRelationshipDTO createRelationshipRequest(Long userId, CreateRelationshipRequest request) {
        log.info("Creating relationship request from user {} to user {}", userId, request.getRelatedUserId());
        
        if (userId.equals(request.getRelatedUserId())) {
            throw new BadRequestException("Cannot create relationship with yourself");
        }
        
        // Check if relationship already exists
        relationshipRepository.findBetweenUsers(userId, request.getRelatedUserId())
            .ifPresent(existing -> {
                throw new BadRequestException("Relationship already exists");
            });
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        
        User relatedUser = userRepository.findById(request.getRelatedUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Related user not found: " + request.getRelatedUserId()));
        
        UserRelationship relationship = UserRelationship.builder()
            .user(user)
            .relatedUser(relatedUser)
            .relationshipType(request.getRelationshipType())
            .status(RelationshipStatus.PENDING)
            .build();
        
        relationship = relationshipRepository.save(relationship);
        
        return toDTO(relationship, userId);
    }

    /**
     * Accept a relationship request
     */
    @Transactional
    public UserRelationshipDTO acceptRelationship(Long userId, Long relationshipId) {
        log.info("User {} accepting relationship {}", userId, relationshipId);
        
        UserRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new ResourceNotFoundException("Relationship not found: " + relationshipId));
        
        // Verify that the current user is the recipient
        if (!relationship.getRelatedUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to accept this relationship");
        }
        
        relationship.setStatus(RelationshipStatus.ACCEPTED);
        relationship = relationshipRepository.save(relationship);
        
        return toDTO(relationship, userId);
    }

    /**
     * Reject a relationship request
     */
    @Transactional
    public void rejectRelationship(Long userId, Long relationshipId) {
        log.info("User {} rejecting relationship {}", userId, relationshipId);
        
        UserRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new ResourceNotFoundException("Relationship not found: " + relationshipId));
        
        // Verify that the current user is the recipient
        if (!relationship.getRelatedUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to reject this relationship");
        }
        
        relationship.setStatus(RelationshipStatus.REJECTED);
        relationshipRepository.save(relationship);
    }

    /**
     * Remove a relationship
     */
    @Transactional
    public void removeRelationship(Long userId, Long relationshipId) {
        log.info("User {} removing relationship {}", userId, relationshipId);
        
        UserRelationship relationship = relationshipRepository.findById(relationshipId)
            .orElseThrow(() -> new ResourceNotFoundException("Relationship not found: " + relationshipId));
        
        // Verify that the current user is part of the relationship
        if (!relationship.getUser().getId().equals(userId) && 
            !relationship.getRelatedUser().getId().equals(userId)) {
            throw new BadRequestException("You are not authorized to remove this relationship");
        }
        
        relationshipRepository.delete(relationship);
    }

    /**
     * Get all relationships for a user
     */
    @Transactional(readOnly = true)
    public List<UserRelationshipDTO> getUserRelationships(Long userId) {
        List<UserRelationship> relationships = relationshipRepository.findAllForUser(userId);
        return relationships.stream()
            .map(r -> toDTO(r, userId))
            .collect(Collectors.toList());
    }

    /**
     * Get pending requests for a user
     */
    @Transactional(readOnly = true)
    public List<UserRelationshipDTO> getPendingRequests(Long userId) {
        List<UserRelationship> pendingRequests = relationshipRepository.findPendingRequestsForUser(userId);
        return pendingRequests.stream()
            .map(r -> toDTO(r, userId))
            .collect(Collectors.toList());
    }

    /**
     * Check if two users are connected
     */
    @Transactional(readOnly = true)
    public boolean areUsersConnected(Long userId1, Long userId2) {
        return relationshipRepository.areUsersConnected(userId1, userId2);
    }

    /**
     * Get IDs of all connected users (friends and family)
     */
    @Transactional(readOnly = true)
    public List<Long> getConnectedUserIds(Long userId) {
        return relationshipRepository.findConnectedUserIds(userId);
    }

    private UserRelationshipDTO toDTO(UserRelationship relationship, Long currentUserId) {
        // Determine who the "other" user is
        boolean isInitiator = relationship.getUser().getId().equals(currentUserId);
        User otherUser = isInitiator ? relationship.getRelatedUser() : relationship.getUser();
        
        return UserRelationshipDTO.builder()
            .id(relationship.getId())
            .userId(relationship.getUser().getId())
            .relatedUserId(otherUser.getId())
            .relatedUserUsername(otherUser.getUsername())
            .relatedUserAvatar(otherUser.getProfilePictureUrl())
            .relationshipType(relationship.getRelationshipType())
            .status(relationship.getStatus())
            .createdAt(relationship.getCreatedAt())
            .build();
    }
}



