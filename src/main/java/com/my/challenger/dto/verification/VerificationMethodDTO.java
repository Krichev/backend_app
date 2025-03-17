package com.my.challenger.dto.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * DTO for challenge verification method
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerificationMethodDTO {
    private String type;
    private Map<String, Object> details;
}
