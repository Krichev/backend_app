package com.my.challenger.repository;

import com.my.challenger.entity.UserAppSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAppSettingsRepository extends JpaRepository<UserAppSettings, Long> {
    Optional<UserAppSettings> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
}
