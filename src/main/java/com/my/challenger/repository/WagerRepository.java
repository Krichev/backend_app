package com.my.challenger.repository;

import com.my.challenger.entity.enums.WagerStatus;
import com.my.challenger.entity.wager.Wager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WagerRepository extends JpaRepository<Wager, Long> {

    List<Wager> findByChallengeId(Long challengeId);

    List<Wager> findByQuizSessionId(Long quizSessionId);

    List<Wager> findByCreatorId(Long creatorId);

    List<Wager> findByStatus(WagerStatus status);

    @Query("SELECT w FROM Wager w JOIN w.participants p WHERE p.user.id = :userId AND w.status = :status")
    List<Wager> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") WagerStatus status);

    @Query("SELECT w FROM Wager w WHERE w.status = 'PROPOSED' AND w.expiresAt < :now")
    List<Wager> findExpiredWagers(@Param("now") LocalDateTime now);

    @Query("SELECT w FROM Wager w JOIN w.participants p WHERE p.user.id = :userId")
    Page<Wager> findByUserId(@Param("userId") Long userId, Pageable pageable);
}
