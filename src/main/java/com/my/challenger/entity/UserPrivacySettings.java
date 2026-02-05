package com.my.challenger.entity;

import com.my.challenger.entity.enums.GenderPreference;
import com.my.challenger.entity.enums.InvitationPreference;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_privacy_settings")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPrivacySettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true,
                foreignKey = @ForeignKey(name = "fk_privacy_user"))
    private User user;

    @Column(name = "allow_requests_from", length = 50)
    @Builder.Default
    private String allowRequestsFrom = "ANYONE";

    @Column(name = "show_connections")
    @Builder.Default
    private Boolean showConnections = true;

    @Column(name = "show_mutual_connections")
    @Builder.Default
    private Boolean showMutualConnections = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "quest_invitation_preference", columnDefinition = "invitation_preference")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private InvitationPreference questInvitationPreference = InvitationPreference.ANYONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender_preference_for_invites", columnDefinition = "gender_preference")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Builder.Default
    private GenderPreference genderPreferenceForInvites = GenderPreference.ANY_GENDER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
