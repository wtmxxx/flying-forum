package com.atcumt.model.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserFollowingDTO {
    private String userId;
    private String cursor;
    private Long lastFollowId;
    private Integer size;
}
