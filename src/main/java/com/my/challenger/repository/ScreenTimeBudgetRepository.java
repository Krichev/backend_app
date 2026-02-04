package com.my.challenger.repository;

import com.my.challenger.entity.ScreenTimeBudget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenTimeBudgetRepository extends JpaRepository<ScreenTimeBudget, Long> {

    Optional<ScreenTimeBudget> findByUserId(Long userId);

    List<ScreenTimeBudget> findByLastResetDateBefore(LocalDate date);

    @Modifying
    @Query("UPDATE ScreenTimeBudget s SET s.availableMinutes = s.dailyBudgetMinutes, " +
            "s.lostTodayMinutes = 0, s.wonTodayMinutes = 0, s.lastResetDate = :date " +
            "WHERE s.lastResetDate < :date")
    void resetDailyBudgets(@Param("date") LocalDate date);

    /**
     * Find users by timezone who need reset (lastResetDate before given date)
     */
    @Query("SELECT s FROM ScreenTimeBudget s WHERE s.timezone = :timezone AND s.lastResetDate < :today")
    List<ScreenTimeBudget> findByTimezoneNeedingReset(
        @Param("timezone") String timezone, 
        @Param("today") LocalDate today,
        Pageable pageable
    );

    /**
     * Count users by timezone needing reset
     */
    @Query("SELECT COUNT(s) FROM ScreenTimeBudget s WHERE s.timezone = :timezone AND s.lastResetDate < :today")
    long countByTimezoneNeedingReset(
        @Param("timezone") String timezone, 
        @Param("today") LocalDate today
    );

    /**
     * Get distinct timezones with users needing reset
     */
    @Query("SELECT DISTINCT s.timezone FROM ScreenTimeBudget s WHERE s.lastResetDate < :today")
    List<String> findDistinctTimezonesNeedingReset(@Param("today") LocalDate today);

    /**
     * Bulk reset for a batch of user IDs
     */
    @Modifying
    @Query("UPDATE ScreenTimeBudget s SET " +
           "s.availableMinutes = s.dailyBudgetMinutes, " +
           "s.lostTodayMinutes = 0, " +
           "s.wonTodayMinutes = 0, " +
           "s.lastResetDate = :today, " +
           "s.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE s.id IN :ids AND s.lastResetDate < :today")
    int bulkResetBudgets(@Param("ids") List<Long> ids, @Param("today") LocalDate today);
}
