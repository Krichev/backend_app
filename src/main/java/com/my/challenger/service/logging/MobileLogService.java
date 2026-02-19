package com.my.challenger.service.logging;

import com.my.challenger.dto.logging.DeviceInfo;
import com.my.challenger.dto.logging.MobileLogEntry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class MobileLogService {

    @Value("${app.mobile-logs.directory:./logs/mobile}")
    private String logDirectory;

    @Value("${app.mobile-logs.retention-days:30}")
    private int retentionDays;

    private final DateTimeFormatter fileDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @PostConstruct
    public void init() {
        File dir = new File(logDirectory);
        if (!dir.exists()) {
            if (dir.mkdirs()) {
                log.info("Created mobile logs directory: {}", logDirectory);
            } else {
                log.error("Failed to create mobile logs directory: {}", logDirectory);
            }
        }
    }

    public synchronized void writeLogs(String sessionId, DeviceInfo deviceInfo, List<MobileLogEntry> logs) {
        String fileName = "mobile-logs-" + LocalDate.now().format(fileDateFormatter) + ".log";
        File logFile = new File(logDirectory, fileName);

        String deviceModel = deviceInfo != null ? deviceInfo.getDeviceModel() : "unknown";
        String appVersion = deviceInfo != null ? deviceInfo.getAppVersion() : "unknown";

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            for (MobileLogEntry entry : logs) {
                String logLine = String.format("[%s] [SESSION:%s] [%s] [%s/%s] %s",
                        entry.getTimestamp(),
                        sessionId,
                        entry.getLevel(),
                        deviceModel,
                        appVersion,
                        entry.getMessage());
                writer.write(logLine);
                writer.newLine();

                if (entry.getStackTrace() != null && !entry.getStackTrace().isEmpty()) {
                    writer.write("	Stack trace:");
                    writer.newLine();
                    String[] stackLines = entry.getStackTrace().split("
");
                    for (String stackLine : stackLines) {
                        writer.write("		" + stackLine);
                        writer.newLine();
                    }
                }
            }
            writer.flush();
        } catch (IOException e) {
            log.error("Error writing mobile logs to file", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void cleanupOldLogs() {
        log.info("Starting cleanup of old mobile logs...");
        File dir = new File(logDirectory);
        if (!dir.exists() || !dir.isDirectory()) return;

        File[] files = dir.listFiles((d, name) -> name.startsWith("mobile-logs-") && name.endsWith(".log"));
        if (files == null) return;

        LocalDate cutoffDate = LocalDate.now().minusDays(retentionDays);
        for (File file : files) {
            try {
                String fileName = file.getName();
                String datePart = fileName.substring("mobile-logs-".length(), fileName.length() - ".log".length());
                LocalDate fileDate = LocalDate.parse(datePart, fileDateFormatter);
                if (fileDate.isBefore(cutoffDate)) {
                    if (file.delete()) {
                        log.info("Deleted old mobile log file: {}", fileName);
                    } else {
                        log.warn("Failed to delete old mobile log file: {}", fileName);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing file for cleanup: {}", file.getName(), e);
            }
        }
    }
}
