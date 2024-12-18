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
public class AppleAuth {
    @TableId
    private String appleAuthId;
    private String userId;
    private String appleId;
    private String appleName;
    private String appleEmail;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
