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
    private String qqAuthId;             // 记录ID，由UUID生成
    private String userId;               // 用户ID
    private String qqOpenid;             // QQ OpenID
    private String qqNickname;           // 昵称
    private LocalDateTime createTime;    // 创建时间
    private LocalDateTime updateTime;    // 更新时间
}
