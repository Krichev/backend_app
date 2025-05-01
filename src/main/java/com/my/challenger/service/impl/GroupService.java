package com.my.challenger.service.impl;

import com.my.challenger.dto.GroupResponseDTO;
import com.my.challenger.entity.Group;
import com.my.challenger.entity.GroupUser;
import com.my.challenger.entity.GroupUserId;
import com.my.challenger.entity.User;
import com.my.challenger.repository.GroupRepository;
import com.my.challenger.repository.GroupUserRepository;
import com.my.challenger.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {
    private final GroupRepository groupRepository;
    private final GroupUserRepository groupUserRepository;
    private final UserRepository userRepository;
    
    /**
     * Get all groups that a user is a member of, with their role in each group
     */
    public List<GroupResponseDTO> getUserGroups(Long userId) {
        // Fetch the user's group memberships
        Iterable<GroupUser> memberships = groupUserRepository.findByIdUserId(userId);
        List<GroupResponseDTO> result = new ArrayList<>();
        
        for (GroupUser membership : memberships) {
            Long groupId = membership.getId().getGroupId();
            Group group = groupRepository.findById(groupId)
                    .orElseThrow(() -> new IllegalStateException("Group not found: " + groupId));
            
            GroupResponseDTO dto = new GroupResponseDTO();
            dto.setId(group.getId());
            dto.setName(group.getName());
            dto.setDescription(group.getDescription());
            dto.setType(group.getType());
            dto.setPrivacy_setting(group.getPrivacySetting());
            dto.setMember_count(group.getMembers().size());
            dto.setCreated_at(group.getCreatedAt());
            dto.setUpdated_at(group.getUpdatedAt());
            dto.setCreator_id(group.getCreator().getId());
            dto.setRole(membership.getRole());
            
            result.add(dto);
        }
        
        return result;
    }
    
    /**
     * Join a group (add user to group)
     */
    public void joinGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("Group not found: " + groupId));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        // Check if user is already a member
        GroupUser existingMembership = groupUserRepository.findByIdGroupIdAndIdUserId(groupId, userId);
        if (existingMembership != null) {
            throw new IllegalStateException("User is already a member of this group");
        }
        
        // Create new membership with MEMBER role
        GroupUser membership = new GroupUser();
        GroupUserId id = new GroupUserId(groupId, userId);
        membership.setId(id);
        membership.setGroup(group);
        membership.setUser(user);
        membership.setRole(com.my.challenger.entity.enums.UserRole.MEMBER);
        membership.setJoinDate(java.time.LocalDateTime.now());
        
        groupUserRepository.save(membership);
    }
}