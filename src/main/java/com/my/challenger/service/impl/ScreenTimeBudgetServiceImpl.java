package com.my.challenger.service.impl;

import com.my.challenger.dto.screentime.*;
import com.my.challenger.entity.ScreenTimeBudget;
import com.my.challenger.entity.User;
import com.my.challenger.exception.InsufficientScreenTimeException;
import com.my.challenger.exception.ResourceNotFoundException;
import com.my.challenger.exception.ScreenTimeLockedException;
import com.my.challenger.repository.ScreenTimeBudgetRepository;
import com.my.challenger.repository.UserRepository;
import com.my.challenger.service.ScreenTimeBudgetService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScreenTimeBudgetServiceImpl implements ScreenTimeBudgetService {

    private final ScreenTimeBudgetRepository screenTimeBudgetRepository;
    private final UserRepository userRepository;
    private final com.my.challenger.repository.UserParentalSettingsRepository parentalRepository;

    @Override
    @Transactional
    public ScreenTimeBudgetDTO getOrCreateBudget(Long userId) {
        ScreenTimeBudget budget = screenTimeBudgetRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultBudget(userId));

        checkAndPerformDailyReset(budget);
        return convertToDTO(budget);
    }

    @Override
    @Transactional
    public ScreenTimeBudgetDTO configureBudget(Long userId, ConfigureBudgetRequest request) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        
        if (budget.isFullyLocked()) {
             throw new ScreenTimeLockedException("Cannot configure budget while time is fully locked");
        }

        budget.setDailyBudgetMinutes(request.getDailyBudgetMinutes());
        
        if (request.getTimezone() != null) {
            try {
                ZoneId.of(request.getTimezone());
                budget.setTimezone(request.getTimezone());
            } catch (DateTimeException e) {
                log.warn("Invalid timezone provided: {}", request.getTimezone());
                // Keep default or previous
            }
        }
        
        // If they increase budget today, they get the difference immediately?
        // Usually budget changes apply next day, or we can adjust available if we want.
        // For simplicity, let's just update the config, and it affects reset. 
        // Or if we want to be nice, we can update available if it increased.
        // Let's stick to requirements: "User can configure their own daily budget". 
        // We won't retroactive update available unless reset happens.
        
        ScreenTimeBudget saved = screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=CONFIGURE dailyBudget={}", userId, saved.getDailyBudgetMinutes());
        
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    @Retryable(value = OptimisticLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    public ScreenTimeBudgetDTO deductTime(Long userId, int minutes) {
        if (minutes <= 0) return getOrCreateBudget(userId);

        ScreenTimeBudget budget = getBudgetEntity(userId);
        
        if (!Boolean.TRUE.equals(budget.getScreenTimeEnabled())) {
            return convertToDTO(budget);
        }

        checkAndPerformDailyReset(budget);

        if (budget.isFullyLocked()) {
            throw new ScreenTimeLockedException("Screen time is locked due to penalty");
        }

        if (budget.getAvailableMinutes() < minutes) {
            throw new InsufficientScreenTimeException("Insufficient screen time available. Requested: " + minutes + ", Available: " + budget.getAvailableMinutes());
        }

        int before = budget.getAvailableMinutes();
        budget.setAvailableMinutes(budget.getAvailableMinutes() - minutes);
        budget.setLastActivityAt(LocalDateTime.now());
        
        ScreenTimeBudget saved = screenTimeBudgetRepository.save(budget);
        
        log.info("SCREEN_TIME_AUDIT userId={} action=DEDUCT minutes={} availableBefore={} availableAfter={}",
                userId, minutes, before, saved.getAvailableMinutes());

        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ScreenTimeBudgetDTO syncUsage(Long userId, SyncTimeRequest request) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        if (!Boolean.TRUE.equals(budget.getScreenTimeEnabled())) {
            return convertToDTO(budget);
        }
        return deductTime(userId, request.getUsedMinutes());
    }

    @Override
    @Transactional(readOnly = true)
    public ScreenTimeStatusDTO getStatus(Long userId) {
        ScreenTimeBudget budget = screenTimeBudgetRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultBudget(userId)); 
        
        return convertToStatusDTO(getOrCreateBudget(userId));
    }

    @Override
    @Transactional
    public void lockTime(Long userId, int minutes) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        checkAndPerformDailyReset(budget);

        int available = budget.getAvailableMinutes();
        
        if (available < minutes) {
            throw new InsufficientScreenTimeException("Cannot lock " + minutes + " minutes. Available: " + available);
        }

        budget.setAvailableMinutes(available - minutes);
        budget.setLockedMinutes(budget.getLockedMinutes() + minutes);
        
        screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=LOCK minutes={}", userId, minutes);
    }

    @Override
    @Transactional
    public void unlockTime(Long userId, int minutes) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        
        if (budget.getLockedMinutes() < minutes) {
            log.warn("Attempting to unlock more minutes than locked. User: {}, Locked: {}, Unlock: {}", 
                    userId, budget.getLockedMinutes(), minutes);
            minutes = budget.getLockedMinutes();
        }

        budget.setLockedMinutes(budget.getLockedMinutes() - minutes);
        budget.setAvailableMinutes(budget.getAvailableMinutes() + minutes);
        
        screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=UNLOCK minutes={}", userId, minutes);
    }

    @Override
    @Transactional
    public void loseTime(Long userId, int minutes) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        checkAndPerformDailyReset(budget);

        int available = budget.getAvailableMinutes();
        int effectiveDeduction = Math.min(available, minutes);
        
        budget.setAvailableMinutes(available - effectiveDeduction);
        budget.setLostTodayMinutes(budget.getLostTodayMinutes() + minutes);
        budget.setTotalLostMinutes(budget.getTotalLostMinutes() + minutes);
        
        screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=LOSE_WAGER minutes={}", userId, minutes);
    }

    @Override
    @Transactional
    public void winTime(Long userId, int minutes) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        checkAndPerformDailyReset(budget);

        budget.setAvailableMinutes(budget.getAvailableMinutes() + minutes);
        budget.setWonTodayMinutes(budget.getWonTodayMinutes() + minutes);
        budget.setTotalWonMinutes(budget.getTotalWonMinutes() + minutes);
        
        screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=WIN_WAGER minutes={}", userId, minutes);
    }

    @Override
    @Transactional
    public void resetDailyBudgets() {
        log.info("Starting daily reset of screen time budgets");
        screenTimeBudgetRepository.resetDailyBudgets(LocalDate.now());
        log.info("Completed daily reset of screen time budgets");
    }

    @Override
    @Transactional
    public ScreenTimeBudgetDTO toggleScreenTime(Long userId, Long callerId, boolean enabled) {
        ScreenTimeBudget budget = getBudgetEntity(userId);
        validateTogglePermission(budget, callerId);
        
        budget.setScreenTimeEnabled(enabled);
        ScreenTimeBudget saved = screenTimeBudgetRepository.save(budget);
        
        log.info("SCREEN_TIME_AUDIT userId={} action=TOGGLE_ENABLED enabled={} calledBy={}", 
                userId, enabled, callerId);
                
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ScreenTimeBudgetDTO delegateControl(Long userId, Long controllerUserId) {
        if (userId.equals(controllerUserId)) {
            throw new IllegalArgumentException("Cannot delegate control to yourself");
        }
        
        User controller = userRepository.findById(controllerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Controller user not found"));
                
        ScreenTimeBudget budget = getBudgetEntity(userId);
        budget.setScreenTimeControlledBy(controller);
        budget.setScreenTimeControlLocked(true);
        
        ScreenTimeBudget saved = screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=DELEGATE_CONTROL controllerId={}", userId, controllerUserId);
        
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ScreenTimeBudgetDTO releaseControl(Long targetUserId, Long callerId) {
        ScreenTimeBudget budget = getBudgetEntity(targetUserId);
        
        if (budget.getScreenTimeControlledBy() == null || !budget.getScreenTimeControlledBy().getId().equals(callerId)) {
            throw new com.my.challenger.exception.UnauthorizedException("Only the current controller can release control");
        }
        
        budget.setScreenTimeControlledBy(null);
        budget.setScreenTimeControlLocked(false);
        
        ScreenTimeBudget saved = screenTimeBudgetRepository.save(budget);
        log.info("SCREEN_TIME_AUDIT userId={} action=RELEASE_CONTROL targetUserId={} releasedBy={}", 
                targetUserId, targetUserId, callerId);
                
        return convertToDTO(saved);
    }

    @Override
    @Transactional
    public ScreenTimeBudgetDTO toggleForControlledUser(Long targetUserId, Long callerId, boolean enabled) {
        ScreenTimeBudget budget = getBudgetEntity(targetUserId);
        
        boolean isController = budget.getScreenTimeControlledBy() != null && budget.getScreenTimeControlledBy().getId().equals(callerId);
        boolean isParent = false;
        
        var parentalSettings = parentalRepository.findByUserId(targetUserId).orElse(null);
        if (parentalSettings != null && Boolean.TRUE.equals(parentalSettings.getIsChildAccount()) && callerId.equals(parentalSettings.getParentUserId())) {
            isParent = true;
        }
        
        if (!isController && !isParent) {
            throw new com.my.challenger.exception.UnauthorizedException("Only the controller or parent can toggle screen time for this user");
        }
        
        budget.setScreenTimeEnabled(enabled);
        ScreenTimeBudget saved = screenTimeBudgetRepository.save(budget);
        
        log.info("SCREEN_TIME_AUDIT userId={} action=CONTROLLED_TOGGLE enabled={} calledBy={}", 
                targetUserId, enabled, callerId);
                
        return convertToDTO(saved);
    }

    private void validateTogglePermission(ScreenTimeBudget budget, Long callerId) {
        if (Boolean.TRUE.equals(budget.getScreenTimeControlLocked())) {
            if (budget.getScreenTimeControlledBy() != null && !budget.getScreenTimeControlledBy().getId().equals(callerId)) {
                // Also check if caller is parent
                var parentalSettings = parentalRepository.findByUserId(budget.getUser().getId()).orElse(null);
                if (parentalSettings != null && Boolean.TRUE.equals(parentalSettings.getIsChildAccount()) && callerId.equals(parentalSettings.getParentUserId())) {
                    return; // Parent has permission
                }
                throw new com.my.challenger.exception.UnauthorizedException("Screen time control is locked. Only the controller or parent can change it.");
            }
        }
    }

    private ScreenTimeBudget getBudgetEntity(Long userId) {
        ScreenTimeBudget budget = screenTimeBudgetRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultBudget(userId));
        
        // Auto-unlock if controller is gone
        if (Boolean.TRUE.equals(budget.getScreenTimeControlLocked()) && budget.getScreenTimeControlledBy() == null) {
            // Check if it's a child account, if so we might need to find current parent
            var parentalSettings = parentalRepository.findByUserId(userId).orElse(null);
            if (parentalSettings != null && Boolean.TRUE.equals(parentalSettings.getIsChildAccount()) && parentalSettings.getParentUserId() != null) {
                userRepository.findById(parentalSettings.getParentUserId()).ifPresent(parent -> {
                    budget.setScreenTimeControlledBy(parent);
                    screenTimeBudgetRepository.save(budget);
                });
            } else {
                budget.setScreenTimeControlLocked(false);
                screenTimeBudgetRepository.save(budget);
                log.info("SCREEN_TIME_AUDIT userId={} action=AUTO_UNLOCK reason=CONTROLLER_NULL", userId);
            }
        }
        
        return budget;
    }

    private ScreenTimeBudget createDefaultBudget(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        ScreenTimeBudget budget = ScreenTimeBudget.builder()
                .user(user)
                .dailyBudgetMinutes(180)
                .availableMinutes(180)
                .lockedMinutes(0)
                .lostTodayMinutes(0)
                .wonTodayMinutes(0)
                .totalLostMinutes(0L)
                .totalWonMinutes(0L)
                .lastResetDate(LocalDate.now())
                .timezone("UTC")
                .screenTimeEnabled(true)
                .screenTimeControlLocked(false)
                .build();
        
        return screenTimeBudgetRepository.save(budget);
    }

    private void checkAndPerformDailyReset(ScreenTimeBudget budget) {
        if (!Boolean.TRUE.equals(budget.getScreenTimeEnabled())) {
            return;
        }

        LocalDate today = LocalDate.now(); 
        
        if (budget.getLastResetDate().isBefore(today)) {
            // Reset logic
            budget.setAvailableMinutes(budget.getDailyBudgetMinutes());
            budget.setLostTodayMinutes(0);
            budget.setWonTodayMinutes(0);
            budget.setLastResetDate(today);
        }
    }

    private ScreenTimeBudgetDTO convertToDTO(ScreenTimeBudget budget) {
        return ScreenTimeBudgetDTO.builder()
                .id(budget.getId())
                .userId(budget.getUser().getId())
                .dailyBudgetMinutes(budget.getDailyBudgetMinutes())
                .availableMinutes(budget.getAvailableMinutes())
                .lockedMinutes(budget.getLockedMinutes())
                .lostMinutes(budget.getLostTodayMinutes())
                .totalWonMinutes(budget.getTotalWonMinutes())
                .totalLostMinutes(budget.getTotalLostMinutes())
                .lastResetDate(budget.getLastResetDate().toString())
                .screenTimeEnabled(Boolean.TRUE.equals(budget.getScreenTimeEnabled()))
                .controlledBy(budget.getScreenTimeControlledBy() != null ? budget.getScreenTimeControlledBy().getId() : null)
                .controllerUsername(budget.getScreenTimeControlledBy() != null ? budget.getScreenTimeControlledBy().getUsername() : null)
                .controlLocked(Boolean.TRUE.equals(budget.getScreenTimeControlLocked()))
                .createdAt(budget.getCreatedAt())
                .updatedAt(budget.getUpdatedAt())
                .build();
    }
    
    private ScreenTimeStatusDTO convertToStatusDTO(ScreenTimeBudgetDTO dto) {
        return ScreenTimeStatusDTO.builder()
                .isLocked(dto.getLockedMinutes() > 0 && dto.getAvailableMinutes() == 0) // or just locked > 0? "isLocked" usually implies blocked.
                .availableMinutes(dto.getAvailableMinutes())
                .build();
    }
    
    // Direct entity to status DTO
    private ScreenTimeStatusDTO convertToStatusDTO(ScreenTimeBudget budget) {
        return ScreenTimeStatusDTO.builder()
                .isLocked(budget.isFullyLocked())
                .availableMinutes(budget.getAvailableMinutes())
                .build();
    }
}
