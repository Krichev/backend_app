package com.my.challenger.repository.vibration;

import com.my.challenger.entity.enums.VibrationSessionStatus;
import com.my.challenger.entity.vibration.VibrationGameSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface VibrationGameSessionRepository extends JpaRepository<VibrationGameSession, UUID> {
    Optional<VibrationGameSession> findByIdAndUserId(UUID id, String userId);
    Optional<VibrationGameSession> findFirstByUserIdAndStatusOrderByStartedAtDesc(String userId, VibrationSessionStatus status);
    Page<VibrationGameSession> findByUserId(String userId, Pageable pageable);
}
