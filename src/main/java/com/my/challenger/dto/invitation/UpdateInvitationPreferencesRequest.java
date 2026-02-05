package com.my.challenger.dto.invitation;

import com.my.challenger.entity.enums.GenderPreference;
import com.my.challenger.entity.enums.InvitationPreference;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInvitationPreferencesRequest {
    @NotNull
    private InvitationPreference questInvitationPreference;
    
    @NotNull
    private GenderPreference genderPreferenceForInvites;
}
