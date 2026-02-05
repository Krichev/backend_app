package com.my.challenger.dto.invitation;

import com.my.challenger.entity.enums.GenderPreference;
import com.my.challenger.entity.enums.InvitationPreference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInvitationPreferencesDTO {
    private Long userId;
    private InvitationPreference questInvitationPreference;
    private GenderPreference genderPreferenceForInvites;
}
