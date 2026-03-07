package com.my.challenger.entity;

import com.my.challenger.entity.enums.TvDisplayStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "tv_displays")
public class TvDisplay {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "pairing_code", nullable = false, unique = true)
    private String pairingCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TvDisplayStatus status; // WAITING, CLAIMED, EXPIRED

    @Column(name = "room_code")
    private String roomCode;

    @Column(name = "tv_user_id")
    private Long tvUserId;

    @Column(name = "claimed_by_user_id")
    private Long claimedByUserId;

    private String token;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (status == null) {
            status = TvDisplayStatus.WAITING;
        }
    }
}
