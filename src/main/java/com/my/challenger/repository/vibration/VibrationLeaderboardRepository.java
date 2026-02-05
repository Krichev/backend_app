package com.my.challenger.repository.vibration;

import com.my.challenger.entity.enums.LeaderboardPeriod;
import com.my.challenger.entity.enums.VibrationDifficulty;
import com.my.challenger.entity.vibration.VibrationLeaderboard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface VibrationLeaderboardRepository extends JpaRepository<VibrationLeaderboard, Long> {
    
    Optional<VibrationLeaderboard> findByUserIdAndPeriodAndPeriodStartAndDifficulty(
            String userId, LeaderboardPeriod period, LocalDate periodStart, VibrationDifficulty difficulty);

    Page<VibrationLeaderboard> findByPeriodAndPeriodStartAndDifficultyOrderByTotalScoreDesc(
            LeaderboardPeriod period, LocalDate periodStart, VibrationDifficulty difficulty, Pageable pageable);

    @Query("SELECT COUNT(v) + 1 FROM VibrationLeaderboard v WHERE v.period = :period AND v.periodStart = :periodStart AND v.difficulty = :difficulty AND v.totalScore > :score")
    Integer findRank(LeaderboardPeriod period, LocalDate periodStart, VibrationDifficulty difficulty, Integer score);
}
