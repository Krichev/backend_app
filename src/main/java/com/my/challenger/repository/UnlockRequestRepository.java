package com.my.challenger.repository;

import com.my.challenger.entity.lock.UnlockRequest;
import com.my.challenger.entity.enums.UnlockRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UnlockRequestRepository extends JpaRepository<UnlockRequest, Long> {
    List<UnlockRequest> findByRequesterIdAndStatus(Long requesterId, UnlockRequestStatus status);
    List<UnlockRequest> findByApproverIdAndStatus(Long approverId, UnlockRequestStatus status);
    List<UnlockRequest> findByPenaltyIdAndStatus(Long penaltyId, UnlockRequestStatus status);

    @Query("SELECT r FROM UnlockRequest r WHERE r.status = 'PENDING' AND r.expiresAt < :now")
    List<UnlockRequest> findExpiredPendingRequests(LocalDateTime now);
}
