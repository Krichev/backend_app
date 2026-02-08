package com.my.challenger.service.impl;

import com.my.challenger.dto.lock.*;
import com.my.challenger.entity.User;
import com.my.challenger.entity.enums.UnlockRequestStatus;
import com.my.challenger.entity.enums.UnlockType;
import com.my.challenger.entity.lock.AccountLockConfig;
import com.my.challenger.entity.lock.UnlockRequest;
import com.my.challenger.entity.penalty.Penalty;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.repository.*;
import com.my.challenger.service.ScreenTimeBudgetService;
import com.my.challenger.service.UnlockRequestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class UnlockRequestServiceImpl implements UnlockRequestService {

    private final UnlockRequestRepository unlockRequestRepository;
    private final AccountLockConfigRepository accountLockConfigRepository;
    private final PenaltyRepository penaltyRepository;
    private final UserRepository userRepository;
    private final ScreenTimeBudgetService screenTimeBudgetService;
    private final ParentalLinkRepository parentalLinkRepository;

    @Override
    @Transactional
    public UnlockRequestDTO createUnlockRequest(Long userId, CreateUnlockRequestDTO request) {
        User requester = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        AccountLockConfig config = getOrCreateConfig(userId);
        
        // Determine approver
        User approver = null;
        Penalty penalty = null;
        
        if (request.getPenaltyId() != null) {
            penalty = penaltyRepository.findById(request.getPenaltyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Penalty not found"));
            
            // Check for duplicate pending requests
            List<UnlockRequest> existing = unlockRequestRepository.findByPenaltyIdAndStatus(penalty.getId(), UnlockRequestStatus.PENDING);
            if (!existing.isEmpty()) {
                throw new IllegalStateException("An unlock request is already pending for this penalty");
            }

            if (config.getRequireApprovalFrom() != null) {
                approver = config.getRequireApprovalFrom();
            } else {
                approver = penalty.getAssignedBy();
            }
        }
        
        // If child, fallback to parent if no approver yet
        if (approver == null && requester.getChildAccount()) {
            approver = parentalLinkRepository.findActiveParentsForChild(userId)
                    .stream()
                    .findFirst()
                    .map(pl -> pl.getParent())
                    .orElse(null);
        }

        UnlockRequest unlockRequest = UnlockRequest.builder()
                .requester(requester)
                .approver(approver)
                .penalty(penalty)
                .unlockType(request.getUnlockType())
                .status(UnlockRequestStatus.PENDING)
                .reason(request.getReason())
                .paymentType(request.getPaymentType())
                .deviceInfo(request.getDeviceInfo())
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build();

        unlockRequest = unlockRequestRepository.save(unlockRequest);
        log.info("UNLOCK_AUDIT userId={} action=CREATE_REQUEST type={} id={}", userId, request.getUnlockType(), unlockRequest.getId());
        
        return mapToDTO(unlockRequest);
    }

    @Override
    public List<UnlockRequestDTO> getMyPendingRequests(Long userId) {
        return unlockRequestRepository.findByRequesterIdAndStatus(userId, UnlockRequestStatus.PENDING)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    public List<UnlockRequestDTO> getRequestsToApprove(Long userId) {
        return unlockRequestRepository.findByApproverIdAndStatus(userId, UnlockRequestStatus.PENDING)
                .stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public UnlockRequestDTO approveUnlockRequest(Long approverId, Long requestId, ApproveUnlockRequestDTO request) {
        UnlockRequest unlockRequest = unlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Unlock request not found"));

        if (!unlockRequest.getApprover().getId().equals(approverId)) {
            throw new IllegalStateException("Not authorized to approve this request");
        }

        if (unlockRequest.getStatus() != UnlockRequestStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING state");
        }

        unlockRequest.setStatus(UnlockRequestStatus.APPROVED);
        unlockRequest.setApproverMessage(request.getMessage());
        unlockRequest.setRespondedAt(LocalDateTime.now());
        
        performUnlock(unlockRequest);

        unlockRequest = unlockRequestRepository.save(unlockRequest);
        log.info("UNLOCK_AUDIT userId={} action=APPROVE_REQUEST requestId={} approverId={}", 
                unlockRequest.getRequester().getId(), requestId, approverId);
        
        return mapToDTO(unlockRequest);
    }

    @Override
    @Transactional
    public UnlockRequestDTO denyUnlockRequest(Long approverId, Long requestId, DenyUnlockRequestDTO request) {
        UnlockRequest unlockRequest = unlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Unlock request not found"));

        if (!unlockRequest.getApprover().getId().equals(approverId)) {
            throw new IllegalStateException("Not authorized to deny this request");
        }

        unlockRequest.setStatus(UnlockRequestStatus.DENIED);
        unlockRequest.setApproverMessage(request.getMessage());
        unlockRequest.setRespondedAt(LocalDateTime.now());

        unlockRequest = unlockRequestRepository.save(unlockRequest);
        log.info("UNLOCK_AUDIT userId={} action=DENY_REQUEST requestId={} approverId={}", 
                unlockRequest.getRequester().getId(), requestId, approverId);
        
        return mapToDTO(unlockRequest);
    }

    @Override
    @Transactional
    public UnlockRequestDTO cancelUnlockRequest(Long userId, Long requestId) {
        UnlockRequest unlockRequest = unlockRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Unlock request not found"));

        if (!unlockRequest.getRequester().getId().equals(userId)) {
            throw new IllegalStateException("Not authorized to cancel this request");
        }

        unlockRequest.setStatus(UnlockRequestStatus.CANCELLED);
        unlockRequest = unlockRequestRepository.save(unlockRequest);
        
        return mapToDTO(unlockRequest);
    }

    @Override
    @Transactional
    public UnlockRequestDTO useEmergencyBypass(Long userId) {
        AccountLockConfig config = getOrCreateConfig(userId);
        
        if (!config.getAllowEmergencyBypass()) {
            throw new IllegalStateException("Emergency bypass is not allowed for this account");
        }

        // Check reset
        checkBypassReset(config);

        if (config.getEmergencyBypassesUsedThisMonth() >= config.getMaxEmergencyBypassesPerMonth()) {
            throw new IllegalStateException("Monthly emergency bypass limit reached");
        }

        config.setEmergencyBypassesUsedThisMonth(config.getEmergencyBypassesUsedThisMonth() + 1);
        accountLockConfigRepository.save(config);

        UnlockRequest unlockRequest = UnlockRequest.builder()
                .requester(config.getUser())
                .unlockType(UnlockType.EMERGENCY_BYPASS)
                .status(UnlockRequestStatus.AUTO_APPROVED)
                .bypassNumber(config.getEmergencyBypassesUsedThisMonth())
                .requestedAt(LocalDateTime.now())
                .respondedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now())
                .build();

        // Unlock all active penalties? 
        // For emergency bypass, we usually unlock screen time.
        screenTimeBudgetService.unlockTime(userId, 1440); // Unlock for a full day equivalent

        unlockRequest = unlockRequestRepository.save(unlockRequest);
        log.info("UNLOCK_AUDIT userId={} action=EMERGENCY_BYPASS bypassNumber={}", userId, config.getEmergencyBypassesUsedThisMonth());
        
        return mapToDTO(unlockRequest);
    }

    @Override
    @Transactional
    public UnlockRequestDTO payPenaltyToUnlock(Long userId, String paymentType, Long penaltyId) {
        AccountLockConfig config = getOrCreateConfig(userId);
        
        if (!config.getAllowSelfUnlock()) {
            throw new IllegalStateException("Self-unlock is not allowed for this account");
        }

        Penalty penalty = penaltyRepository.findById(penaltyId)
                .orElseThrow(() -> new ResourceNotFoundException("Penalty not found"));

        if (penalty.getScreenTimeMinutes() == null || penalty.getScreenTimeMinutes() <= 0) {
            throw new IllegalStateException("This penalty does not have screen time lock to pay for");
        }

        // Calculate cost
        BigDecimal multiplier = config.getUnlockPenaltyMultiplier();
        int minutesToUnlock = penalty.getScreenTimeMinutes();
        
        if ("POINTS".equalsIgnoreCase(paymentType)) {
            long cost = (long) (minutesToUnlock * multiplier.doubleValue() * 10); // Example: 10 points per minute * multiplier
            User user = config.getUser();
            if (user.getPoints() < cost) {
                throw new IllegalStateException("Insufficient points to unlock. Required: " + cost);
            }
            user.deductPoints(cost);
            userRepository.save(user);
        } else if ("SCREEN_TIME".equalsIgnoreCase(paymentType)) {
            int deduction = (int) (minutesToUnlock * multiplier.doubleValue());
            // This will be deducted from tomorrow's budget? Or current available?
            // Requirement says "deduct from future budget". 
            // For now let's just use loseTime which reduces available/increases total lost.
            screenTimeBudgetService.loseTime(userId, deduction);
        } else {
            throw new IllegalArgumentException("Unsupported payment type: " + paymentType);
        }

        UnlockRequest unlockRequest = UnlockRequest.builder()
                .requester(config.getUser())
                .penalty(penalty)
                .unlockType(UnlockType.PENALTY_PAYMENT)
                .status(UnlockRequestStatus.AUTO_APPROVED)
                .paymentType(paymentType)
                .paymentFulfilled(true)
                .requestedAt(LocalDateTime.now())
                .respondedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now())
                .build();

        screenTimeBudgetService.unlockTime(userId, minutesToUnlock);
        
        unlockRequest = unlockRequestRepository.save(unlockRequest);
        log.info("UNLOCK_AUDIT userId={} action=PAY_PENALTY penaltyId={} paymentType={}", userId, penaltyId, paymentType);

        return mapToDTO(unlockRequest);
    }

    @Override
    public AccountLockConfigDTO getMyLockConfig(Long userId) {
        return mapToDTO(getOrCreateConfig(userId));
    }

    @Override
    @Transactional
    public AccountLockConfigDTO updateMyLockConfig(Long userId, AccountLockConfigDTO dto) {
        User user = userRepository.findById(userId).get();
        if (user.getChildAccount()) {
            throw new IllegalStateException("Children cannot update their own lock configuration");
        }

        AccountLockConfig config = getOrCreateConfig(userId);
        config.setAllowSelfUnlock(dto.getAllowSelfUnlock());
        config.setAllowEmergencyBypass(dto.getAllowEmergencyBypass());
        config.setMaxEmergencyBypassesPerMonth(dto.getMaxEmergencyBypassesPerMonth());
        config.setUnlockPenaltyMultiplier(dto.getUnlockPenaltyMultiplier());
        config.setEscalationEnabled(dto.getEscalationEnabled());
        config.setEscalationAfterAttempts(dto.getEscalationAfterAttempts());
        
        if (dto.getRequireApprovalFrom() != null) {
            config.setRequireApprovalFrom(userRepository.findById(dto.getRequireApprovalFrom()).orElse(null));
        } else {
            config.setRequireApprovalFrom(null);
        }

        config = accountLockConfigRepository.save(config);
        return mapToDTO(config);
    }

    @Override
    public AccountLockConfigDTO getChildLockConfig(Long parentId, Long childId) {
        validateParentChildLink(parentId, childId);
        return mapToDTO(getOrCreateConfig(childId));
    }

    @Override
    @Transactional
    public AccountLockConfigDTO updateChildLockConfig(Long parentId, Long childId, AccountLockConfigDTO dto) {
        validateParentChildLink(parentId, childId);
        User parent = userRepository.findById(parentId).get();
        
        AccountLockConfig config = getOrCreateConfig(childId);
        config.setConfiguredBy(parent);
        config.setAllowSelfUnlock(dto.getAllowSelfUnlock());
        config.setAllowEmergencyBypass(dto.getAllowEmergencyBypass());
        config.setMaxEmergencyBypassesPerMonth(dto.getMaxEmergencyBypassesPerMonth());
        config.setUnlockPenaltyMultiplier(dto.getUnlockPenaltyMultiplier());
        config.setEscalationEnabled(dto.getEscalationEnabled());
        config.setEscalationAfterAttempts(dto.getEscalationAfterAttempts());
        
        config = accountLockConfigRepository.save(config);
        return mapToDTO(config);
    }

    @Override
    @Transactional
    public void expirePendingRequests() {
        List<UnlockRequest> expired = unlockRequestRepository.findExpiredPendingRequests(LocalDateTime.now());
        for (UnlockRequest req : expired) {
            req.setStatus(UnlockRequestStatus.EXPIRED);
            unlockRequestRepository.save(req);
        }
    }

    @Override
    public AccountLockConfig getOrCreateConfig(Long userId) {
        return accountLockConfigRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                    
                    AccountLockConfig config = AccountLockConfig.builder()
                            .user(user)
                            .allowSelfUnlock(!user.getChildAccount())
                            .allowEmergencyBypass(!user.getChildAccount())
                            .maxEmergencyBypassesPerMonth(user.getChildAccount() ? 0 : 3)
                            .unlockPenaltyMultiplier(new BigDecimal("2.00"))
                            .build();
                    
                    return accountLockConfigRepository.save(config);
                });
    }

    private void performUnlock(UnlockRequest request) {
        if (request.getPenalty() != null && request.getPenalty().getScreenTimeMinutes() != null) {
            screenTimeBudgetService.unlockTime(request.getRequester().getId(), request.getPenalty().getScreenTimeMinutes());
        } else {
            // If no specific penalty, unlock for a full day (failsafe)
            screenTimeBudgetService.unlockTime(request.getRequester().getId(), 1440);
        }
    }

    private void checkBypassReset(AccountLockConfig config) {
        LocalDate now = LocalDate.now();
        LocalDate firstOfMonth = now.withDayOfMonth(1);
        if (config.getBypassMonthResetDate().isBefore(firstOfMonth)) {
            config.setEmergencyBypassesUsedThisMonth(0);
            config.setBypassMonthResetDate(now);
        }
    }

    private void validateParentChildLink(Long parentId, Long childId) {
        if (!parentalLinkRepository.existsByParentIdAndChildIdAndStatus(parentId, childId, com.my.challenger.entity.parental.ParentalLinkStatus.ACTIVE)) {
            throw new IllegalStateException("No active parental link found between parent " + parentId + " and child " + childId);
        }
    }

    private UnlockRequestDTO mapToDTO(UnlockRequest request) {
        return UnlockRequestDTO.builder()
                .id(request.getId())
                .requesterId(request.getRequester().getId())
                .requesterUsername(request.getRequester().getUsername())
                .approverId(request.getApprover() != null ? request.getApprover().getId() : null)
                .approverUsername(request.getApprover() != null ? request.getApprover().getUsername() : null)
                .penaltyId(request.getPenalty() != null ? request.getPenalty().getId() : null)
                .unlockType(request.getUnlockType())
                .status(request.getStatus())
                .paymentType(request.getPaymentType())
                .paymentAmount(request.getPaymentAmount())
                .paymentFulfilled(request.getPaymentFulfilled())
                .bypassNumber(request.getBypassNumber())
                .reason(request.getReason())
                .approverMessage(request.getApproverMessage())
                .requestedAt(request.getRequestedAt())
                .respondedAt(request.getRespondedAt())
                .expiresAt(request.getExpiresAt())
                .deviceInfo(request.getDeviceInfo())
                .build();
    }

    private AccountLockConfigDTO mapToDTO(AccountLockConfig config) {
        return AccountLockConfigDTO.builder()
                .userId(config.getUser().getId())
                .configuredBy(config.getConfiguredBy() != null ? config.getConfiguredBy().getId() : null)
                .allowSelfUnlock(config.getAllowSelfUnlock())
                .allowEmergencyBypass(config.getAllowEmergencyBypass())
                .maxEmergencyBypassesPerMonth(config.getMaxEmergencyBypassesPerMonth())
                .unlockPenaltyMultiplier(config.getUnlockPenaltyMultiplier())
                .requireApprovalFrom(config.getRequireApprovalFrom() != null ? config.getRequireApprovalFrom().getId() : null)
                .escalationEnabled(config.getEscalationEnabled())
                .escalationAfterAttempts(config.getEscalationAfterAttempts())
                .emergencyBypassesUsedThisMonth(config.getEmergencyBypassesUsedThisMonth())
                .emergencyBypassesRemaining(config.getMaxEmergencyBypassesPerMonth() - config.getEmergencyBypassesUsedThisMonth())
                .bypassMonthResetDate(config.getBypassMonthResetDate())
                .build();
    }
}
