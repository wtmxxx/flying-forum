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
public class UserAuth {
    @TableId
    private String userId;         // UserID，由UUID生成
    private String studentId;      // 学生学号
    private String username;       // 用户名
    private String email;          // 邮箱
    private String password;       // 密码（加密存储）
    private String oauthId;        // OAuthID
    private Boolean enabled;        // 账户是否启用
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
    private LocalDateTime lastLoginTime; // 最后登录时间
}
