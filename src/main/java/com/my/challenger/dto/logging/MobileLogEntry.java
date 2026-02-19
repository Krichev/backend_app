package com.my.challenger.dto.logging;

import lombok.Data;

@Data
public class MobileLogEntry {
    private String level;       // LOG, WARN, ERROR
    private String message;     // The actual log message
    private String timestamp;   // ISO-8601 from client
    private String stackTrace;  // Optional, for errors
}
