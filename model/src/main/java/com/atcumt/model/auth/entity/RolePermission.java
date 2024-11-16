package com.atcumt.model.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RolePermission {
    @TableId
    private String rolePermissionId;
    private String roleId;
    private String permissionId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
