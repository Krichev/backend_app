package com.my.challenger.entity.challenge;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "photo_verification_details")
public class PhotoVerificationDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "requires_photo_comparison")
    private Boolean requiresPhotoComparison = false;

    @Column(name = "verification_mode")
    private String verificationMode = "standard";

    // Audit fields
    @Column(name = "created_at")
    private java.time.LocalDateTime createdAt;

    @Column(name = "updated_at")
    private java.time.LocalDateTime updatedAt;

    // Bidirectional relationship with VerificationDetails
    @OneToOne(mappedBy = "photoDetails")
    private VerificationDetails verificationDetails;

    @PrePersist
    protected void onCreate() {
        createdAt = java.time.LocalDateTime.now();
        updatedAt = java.time.LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = java.time.LocalDateTime.now();
    }
}