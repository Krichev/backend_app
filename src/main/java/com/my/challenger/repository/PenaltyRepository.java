package com.my.challenger.repository;

import com.my.challenger.entity.enums.PenaltyStatus;
import com.my.challenger.entity.penalty.Penalty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PenaltyRepository extends JpaRepository<Penalty, Long> {

    List<Penalty> findByAssignedToId(Long userId);
    
    Page<Penalty> findByAssignedToId(Long userId, Pageable pageable);

    List<Penalty> findByAssignedToIdAndStatus(Long userId, PenaltyStatus status);
    
    Page<Penalty> findByAssignedToIdAndStatus(Long userId, PenaltyStatus status, Pageable pageable);

    @Query("SELECT p FROM Penalty p WHERE p.assignedBy.id = :userId AND p.status IN ('IN_PROGRESS', 'PENDING')")
    List<Penalty> findPenaltiesToReview(@Param("userId") Long userId);

    List<Penalty> findByWagerId(Long wagerId);

    List<Penalty> findByChallengeId(Long challengeId);

    @Query("SELECT p FROM Penalty p WHERE p.status IN ('PENDING', 'IN_PROGRESS') AND p.dueDate < :now")
    List<Penalty> findOverduePenalties(@Param("now") LocalDateTime now);

    long countByAssignedToIdAndStatus(Long userId, PenaltyStatus status);
}
