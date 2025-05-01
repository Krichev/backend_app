package com.my.challenger.repository;

import com.my.challenger.entity.Group;
import com.my.challenger.entity.GroupUser;
import com.my.challenger.entity.GroupUserId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
}