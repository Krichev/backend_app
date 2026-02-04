package com.my.challenger.scheduler;

import com.my.challenger.entity.ScreenTimeBudget;
import com.my.challenger.repository.ScreenTimeBudgetRepository;
import com.my.challenger.service.ScreenTimeResetService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ScreenTimeBudgetResetSchedulerTest {

    @Autowired
    private ScreenTimeResetService resetService;

    @Autowired
    private ScreenTimeBudgetRepository budgetRepository;

    private ScreenTimeBudget createTestBudget(String timezone, LocalDate lastResetDate) {
        ScreenTimeBudget budget = new ScreenTimeBudget();
        // Since we are using an actual DB or H2, we should let the ID be generated or ensure unique
        // For @Transactional tests, saving will generate ID if configured.
        // Assuming ScreenTimeBudget has @Id @GeneratedValue
        budget.setUserId(System.currentTimeMillis()); // Mock User ID
        budget.setDailyBudgetMinutes(180);
        budget.setAvailableMinutes(0); // Needs reset
        budget.setLockedMinutes(0);
        budget.setLostTodayMinutes(50);
        budget.setWonTodayMinutes(20);
        budget.setTimezone(timezone);
        budget.setLastResetDate(lastResetDate);
        return budget;
    }

    @Test
    void shouldResetBudgetsForTimezone() {
        // Given: A user with budget needing reset
        ScreenTimeBudget budget = createTestBudget("UTC", LocalDate.now().minusDays(1));
        budgetRepository.save(budget);

        // When
        int count = resetService.resetBudgetsForTimezone("UTC", LocalDate.now());

        // Then
        assertThat(count).isEqualTo(1);
        ScreenTimeBudget updated = budgetRepository.findById(budget.getId()).orElseThrow();
        assertThat(updated.getAvailableMinutes()).isEqualTo(updated.getDailyBudgetMinutes());
        assertThat(updated.getLostTodayMinutes()).isEqualTo(0);
        assertThat(updated.getLastResetDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void shouldNotResetAlreadyResetBudgets() {
        // Given: A user with budget already reset today
        ScreenTimeBudget budget = createTestBudget("UTC", LocalDate.now());
        budget.setAvailableMinutes(50); // Already used some
        budgetRepository.save(budget);

        // When
        int count = resetService.resetBudgetsForTimezone("UTC", LocalDate.now());

        // Then
        assertThat(count).isEqualTo(0);
        ScreenTimeBudget unchanged = budgetRepository.findById(budget.getId()).orElseThrow();
        assertThat(unchanged.getAvailableMinutes()).isEqualTo(50); // Unchanged
    }

    @Test
    void shouldPreserveLockedMinutes() {
        // Given: A user with locked minutes from penalties
        ScreenTimeBudget budget = createTestBudget("UTC", LocalDate.now().minusDays(1));
        budget.setLockedMinutes(60);
        budgetRepository.save(budget);

        // When
        resetService.resetBudgetsForTimezone("UTC", LocalDate.now());

        // Then
        ScreenTimeBudget updated = budgetRepository.findById(budget.getId()).orElseThrow();
        assertThat(updated.getLockedMinutes()).isEqualTo(60); // Still locked
    }
}