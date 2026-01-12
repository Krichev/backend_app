package com.my.challenger.service.impl;

import com.my.challenger.dto.quiz.ContactGroupDTO;
import com.my.challenger.dto.quiz.CreateContactGroupRequest;
import com.my.challenger.dto.quiz.UpdateContactGroupRequest;
import com.my.challenger.entity.ContactGroup;
import com.my.challenger.entity.User;
import com.my.challenger.entity.UserRelationship;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.exception.BadRequestException;
import com.my.challenger.repository.ContactGroupRepository;
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
public class ContactGroupService {

    private final ContactGroupRepository groupRepository;
    private final UserRelationshipRepository relationshipRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ContactGroupDTO> getUserGroups(Long userId) {
        return groupRepository.findByUserId(userId).stream()
            .map(this::toDTO)
            .collect(Collectors.toList());
    }

    @Transactional
    public ContactGroupDTO createGroup(Long userId, CreateContactGroupRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        ContactGroup group = ContactGroup.builder()
            .user(user)
            .name(request.getName())
            .color(request.getColor())
            .icon(request.getIcon())
            .build();

        return toDTO(groupRepository.save(group));
    }

    @Transactional
    public ContactGroupDTO updateGroup(Long userId, Long groupId, UpdateContactGroupRequest request) {
        ContactGroup group = groupRepository.findByIdAndUserId(groupId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        if (request.getName() != null) group.setName(request.getName());
        if (request.getColor() != null) group.setColor(request.getColor());
        if (request.getIcon() != null) group.setIcon(request.getIcon());

        return toDTO(groupRepository.save(group));
    }

    @Transactional
    public void deleteGroup(Long userId, Long groupId) {
        ContactGroup group = groupRepository.findByIdAndUserId(groupId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found"));
        groupRepository.delete(group);
    }

    @Transactional
    public void addMembers(Long userId, Long groupId, List<Long> relationshipIds) {
        ContactGroup group = groupRepository.findByIdAndUserId(groupId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        List<UserRelationship> relationships = relationshipRepository.findAllById(relationshipIds);
        
        // Filter to ensure all relationships belong to the user
        for (UserRelationship rel : relationships) {
            if (!rel.getUser().getId().equals(userId) && !rel.getRelatedUser().getId().equals(userId)) {
                throw new BadRequestException("Unauthorized access to relationship " + rel.getId());
            }
        }

        group.getRelationships().addAll(relationships);
        groupRepository.save(group);
    }

    @Transactional
    public void removeMember(Long userId, Long groupId, Long relationshipId) {
        ContactGroup group = groupRepository.findByIdAndUserId(groupId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Group not found"));

        group.getRelationships().removeIf(rel -> rel.getId().equals(relationshipId));
        groupRepository.save(group);
    }

    private ContactGroupDTO toDTO(ContactGroup group) {
        return ContactGroupDTO.builder()
            .id(group.getId())
            .name(group.getName())
            .color(group.getColor())
            .icon(group.getIcon())
            .memberCount(group.getRelationships() != null ? group.getRelationships().size() : 0)
            .build();
    }
}
