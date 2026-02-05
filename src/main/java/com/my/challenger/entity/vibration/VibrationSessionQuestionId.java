package com.my.challenger.entity.vibration;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VibrationSessionQuestionId implements Serializable {
    private UUID sessionId;
    private Long songId;
}
