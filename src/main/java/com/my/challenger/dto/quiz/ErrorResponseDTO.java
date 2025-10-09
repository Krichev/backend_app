// src/main/java/com/my/challenger/dto/quiz/ErrorResponseDTO.java
package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard error response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponseDTO {
    
    private Integer status;
    private String error;
    private String message;
    private String path;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}