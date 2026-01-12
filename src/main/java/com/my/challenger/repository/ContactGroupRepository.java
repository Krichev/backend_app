package com.my.challenger.repository;

import com.my.challenger.entity.ContactGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContactGroupRepository extends JpaRepository<ContactGroup, Long> {
    List<ContactGroup> findByUserId(Long userId);
    Optional<ContactGroup> findByIdAndUserId(Long id, Long userId);
}
