package com.my.challenger.repository;

import com.my.challenger.entity.AppVersionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppVersionConfigRepository extends JpaRepository<AppVersionConfig, Long> {
    Optional<AppVersionConfig> findByPlatform(String platform);
}
