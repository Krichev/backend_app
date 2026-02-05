package com.my.challenger.dto.user;

import com.my.challenger.entity.enums.Gender;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserGenderRequest {
    @NotNull
    private Gender gender;
}
