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
public class PermissionVO {
    private String permissionId;
    private String permissionName;
    private String description;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}