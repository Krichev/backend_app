package com.my.challenger.service;

import com.my.challenger.dto.lock.*;
import com.my.challenger.entity.lock.AccountLockConfig;

import java.util.List;

public interface UnlockRequestService {
    UnlockRequestDTO createUnlockRequest(Long userId, CreateUnlockRequestDTO request);
    List<UnlockRequestDTO> getMyPendingRequests(Long userId);
    List<UnlockRequestDTO> getRequestsToApprove(Long userId);
    UnlockRequestDTO approveUnlockRequest(Long approverId, Long requestId, ApproveUnlockRequestDTO request);
    UnlockRequestDTO denyUnlockRequest(Long approverId, Long requestId, DenyUnlockRequestDTO request);
    UnlockRequestDTO cancelUnlockRequest(Long userId, Long requestId);
    UnlockRequestDTO useEmergencyBypass(Long userId);
    UnlockRequestDTO payPenaltyToUnlock(Long userId, String paymentType, Long penaltyId);
    
    AccountLockConfigDTO getMyLockConfig(Long userId);
    AccountLockConfigDTO updateMyLockConfig(Long userId, AccountLockConfigDTO config);
    AccountLockConfigDTO getChildLockConfig(Long parentId, Long childId);
    AccountLockConfigDTO updateChildLockConfig(Long parentId, Long childId, AccountLockConfigDTO config);
    
    void expirePendingRequests();
    
    AccountLockConfig getOrCreateConfig(Long userId);
}
