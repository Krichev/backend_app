package com.my.challenger.repository;

import com.my.challenger.entity.lock.AccountLockConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AccountLockConfigRepository extends JpaRepository<AccountLockConfig, Long> {
    Optional<AccountLockConfig> findByUserId(Long userId);
}
