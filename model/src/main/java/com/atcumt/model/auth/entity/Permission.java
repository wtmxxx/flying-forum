package com.atcumt.model.auth.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 权限实体类
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Permission {
    @TableId
    private String permissionId; // 权限ID，由UUID生成
    private String permissionName; // 权限名称，如查看用户、编辑用户、删除用户
    private String description; // 权限描述，便于说明权限的作用
    private LocalDateTime createTime; // 权限创建时间
    private LocalDateTime updateTime;
}
