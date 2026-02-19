package com.my.challenger.web.controllers;

import com.my.challenger.entity.ScreenTimeResetLog;
import com.my.challenger.repository.ScreenTimeResetLogRepository;
import com.my.challenger.scheduler.ScreenTimeBudgetResetScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/scheduler")
@RequiredArgsConstructor
@Tag(name = "Admin - Scheduler", description = "Admin endpoints for scheduler management")
@PreAuthorize("hasRole('ADMIN')")
public class AdminSchedulerController {

    private final ScreenTimeBudgetResetScheduler resetScheduler;
    private final ScreenTimeResetLogRepository resetLogRepository;

    @PostMapping("/screen-time/reset/{timezone}")
    @Operation(summary = "Manually trigger screen time reset for a timezone")
    public ResponseEntity<Map<String, Object>> triggerReset(@PathVariable String timezone) {
        int count = resetScheduler.triggerManualReset(timezone);
        return ResponseEntity.ok(Map.of(
                "timezone", timezone,
                "usersReset", count,
                "message", "Reset completed successfully"
        ));
    }

    @GetMapping("/screen-time/reset-logs")
    @Operation(summary = "Get reset logs for a date")
    public ResponseEntity<List<ScreenTimeResetLog>> getResetLogs(
            @RequestParam(required = false) LocalDate date) {
        LocalDate queryDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(resetLogRepository.findByResetDate(queryDate));
    }

    @GetMapping("/screen-time/reset-logs/failed")
    @Operation(summary = "Get failed reset logs")
    public ResponseEntity<List<ScreenTimeResetLog>> getFailedLogs() {
        return ResponseEntity.ok(
                resetLogRepository.findByStatusAndResetDate("FAILED", LocalDate.now())
        );
    }
}
