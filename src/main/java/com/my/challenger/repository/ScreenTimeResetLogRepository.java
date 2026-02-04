package com.my.challenger.repository;

import com.my.challenger.entity.ScreenTimeResetLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScreenTimeResetLogRepository extends JpaRepository<ScreenTimeResetLog, Long> {

    Optional<ScreenTimeResetLog> findByResetDateAndTimezone(LocalDate resetDate, String timezone);

    List<ScreenTimeResetLog> findByResetDate(LocalDate resetDate);

    @Query("SELECT DISTINCT s.timezone FROM ScreenTimeResetLog s WHERE s.resetDate = :date AND s.status = 'SUCCESS'")
    List<String> findSuccessfullyResetTimezones(LocalDate date);

    List<ScreenTimeResetLog> findByStatusAndResetDate(String status, LocalDate resetDate);
}
