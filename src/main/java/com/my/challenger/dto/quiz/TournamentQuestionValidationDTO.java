package com.my.challenger.dto.quiz;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of question validation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentQuestionValidationDTO {
    
    private Integer questionId;
    private Boolean isValid;
    
    @Builder.Default
    private List<ValidationError> errors = new ArrayList<>();
    
    @Builder.Default
    private List<ValidationWarning> warnings = new ArrayList<>();
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String errorCode;
        private String message;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationWarning {
        private String field;
        private String warningCode;
        private String message;
    }
    
    public void addError(String field, String errorCode, String message) {
        errors.add(new ValidationError(field, errorCode, message));
        isValid = false;
    }
    
    public void addWarning(String field, String warningCode, String message) {
        warnings.add(new ValidationWarning(field, warningCode, message));
    }
    
    public boolean hasErrors() {
        return errors != null && !errors.isEmpty();
    }
    
    public boolean hasWarnings() {
        return warnings != null && !warnings.isEmpty();
    }
}