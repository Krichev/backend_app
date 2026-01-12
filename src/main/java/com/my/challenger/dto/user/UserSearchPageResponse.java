package com.my.challenger.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSearchPageResponse {
    private List<UserSearchResponse> content;
    private long totalElements;
    private int page;
    private int size;
}
