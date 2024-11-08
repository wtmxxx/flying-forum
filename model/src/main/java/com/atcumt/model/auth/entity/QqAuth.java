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
public class QqAuth {
    @TableId
    private String qqAuthId;       // 记录ID，由UUID生成
    private String authId;         // 关联auth表的ID
    private String openid;         // QQ OpenID
    private String accessToken;    // QQ Access Token
    private String refreshToken;   // QQ Refresh Token
    private Integer expiresIn;     // Access Token有效期（秒）
    private String scope;          // 授权作用域
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}
