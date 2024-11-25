package com.atcumt.model.auth.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RoleVO {
    private String roleId;
    private String roleName;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
