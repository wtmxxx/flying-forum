package com.atcumt.model.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Auth {
    private String id; // 使用 UUID 字符串作为主键
    private String studentId; // 学号
    private String username; // 用户名
    private String email; // 邮箱
    private String password; // 加密后的密码
    private String qqOauthId; // QQ OAuth ID
    private String wechatOauthId; // 微信 OAuth ID
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 更新时间
}
