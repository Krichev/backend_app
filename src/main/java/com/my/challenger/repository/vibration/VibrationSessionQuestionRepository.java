package com.my.challenger.repository.vibration;

import com.my.challenger.entity.vibration.VibrationSessionQuestion;
import com.my.challenger.entity.vibration.VibrationSessionQuestionId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.Optional;

@Repository
public interface VibrationSessionQuestionRepository extends JpaRepository<VibrationSessionQuestion, VibrationSessionQuestionId> {
    Optional<VibrationSessionQuestion> findBySessionIdAndSongId(UUID sessionId, Long songId);
}
