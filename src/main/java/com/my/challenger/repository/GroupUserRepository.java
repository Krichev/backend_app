package com.my.challenger.repository;

import com.my.challenger.entity.GroupUser;
import com.my.challenger.entity.GroupUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupUserRepository extends JpaRepository<GroupUser, GroupUserId> {
    /**
     * Find group membership by group ID and user ID
     */
    GroupUser findByIdGroupIdAndIdUserId(Long groupId, Long userId);
    
    /**
     * Find all group memberships for a user
     */
    Iterable<GroupUser> findByIdUserId(Long userId);
}