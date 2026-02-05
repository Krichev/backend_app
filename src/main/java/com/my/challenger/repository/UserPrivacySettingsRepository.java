package com.my.challenger.repository;

import com.my.challenger.entity.UserPrivacySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPrivacySettingsRepository extends JpaRepository<UserPrivacySettings, Long> {
    Optional<UserPrivacySettings> findByUserId(Long userId);
    
    List<UserPrivacySettings> findByUserIdIn(List<Long> userIds);
}
