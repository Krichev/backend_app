package com.my.challenger.service;

import com.my.challenger.dto.screentime.*;

public interface ScreenTimeBudgetService {

    ScreenTimeBudgetDTO getOrCreateBudget(Long userId);

    ScreenTimeBudgetDTO configureBudget(Long userId, ConfigureBudgetRequest request);

    ScreenTimeBudgetDTO deductTime(Long userId, int minutes);

    ScreenTimeBudgetDTO syncUsage(Long userId, SyncTimeRequest request);

    ScreenTimeStatusDTO getStatus(Long userId);

    void lockTime(Long userId, int minutes);

    void unlockTime(Long userId, int minutes);

    void loseTime(Long userId, int minutes);

    void winTime(Long userId, int minutes);

    void resetDailyBudgets();
}
